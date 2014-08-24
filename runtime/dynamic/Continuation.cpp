#include "dynamic/Continuation.h"

#include <cassert>
#include <cstdlib>

#include "binding/RecordLikeCell.h"
#include "dynamic/State.h"

namespace lliby
{
namespace dynamic
{

namespace
{
	const int ContinuationResumeCookie = 1;

	void relocateShadowStackEntry(alloc::ShadowStackEntry *&stackEntry, std::ptrdiff_t offset)
	{
		if (stackEntry != nullptr)
		{
			// Cast to char* so we do byte-based pointer arithmetic
			stackEntry = reinterpret_cast<alloc::ShadowStackEntry*>(reinterpret_cast<char*>(stackEntry) + offset);
		}
	}

	void relocateShadowStack(alloc::ShadowStackEntry *&stackHead, std::ptrdiff_t offset)
	{
		if (stackHead != nullptr)
		{
			relocateShadowStackEntry(stackHead, offset);

			for(alloc::ShadowStackEntry *entry = stackHead;
				entry != nullptr;
				entry = entry->next)
			{
				relocateShadowStackEntry(entry->next, offset);
			}
		}
	}
}

Continuation::CaptureResult Continuation::capture(World &world)
{
	int currentStackCanary;
	void *stackPointer = &currentStackCanary;
	
	ptrdiff_t stackSize = static_cast<char*>(world.continuationBase) - static_cast<char*>(stackPointer);
	assert(stackSize > 0);

	// Allocate a continuation
	// Use allocatedRecorData because this is used as the closure for EscapeProcedureCell
	auto *cont = reinterpret_cast<Continuation*>(RecordLikeCell::allocateRecordData(sizeof(Continuation) + stackSize));

	// Copy the entire stack over
	memcpy(cont->m_savedStack, static_cast<char*>(world.continuationBase) - stackSize, stackSize); 
	
	// Record the stack size
	cont->m_savedStackBytes = stackSize;

	// Calculate the offset between the old stack and the new saved heap
	std::ptrdiff_t relocationOffset = &cont->m_savedStack[stackSize] - static_cast<char*>(world.continuationBase);
	
	// Save metadata from the world	
	cont->m_shadowStackHead = world.shadowStackHead;
	relocateShadowStack(cont->m_shadowStackHead, relocationOffset);

	cont->m_strongRefs = *world.strongRefs;
	cont->m_strongRefs.relocate(relocationOffset, stackPointer, world.continuationBase);

	cont->m_weakRefs = *world.weakRefs;
	cont->m_weakRefs.relocate(relocationOffset, stackPointer, world.continuationBase);

	cont->m_dynamicState = world.activeState;

	// Finally set the jump target
	const int jumpResult = setjmp(cont->m_jumpTarget);

	if (jumpResult == 0)
	{
		return {
			.continuation = cont,
			.passedValue = nullptr
		};
	}
	else if (jumpResult == ContinuationResumeCookie)
	{
		// Our stack is completely bogus for everything after "currentStackCanary"
		// We need to rebuild the values we use here
		cont = const_cast<Continuation*>(world.resumingContinuation);
		stackSize = cont->m_savedStackBytes;
		std::ptrdiff_t delocationOffset = static_cast<char*>(world.continuationBase) -  &cont->m_savedStack[stackSize];
	
		// Assign our GC state back in to the world and delocate it back in to place
		world.shadowStackHead = cont->m_shadowStackHead;
		relocateShadowStack(world.shadowStackHead, delocationOffset);

		(*world.strongRefs) = cont->m_strongRefs;
		world.strongRefs->relocate(delocationOffset, &cont->m_savedStack[0], &cont->m_savedStack[stackSize]);

		(*world.weakRefs) = cont->m_weakRefs;
		world.weakRefs->relocate(delocationOffset, &cont->m_savedStack[0], &cont->m_savedStack[stackSize]);

		// Root our passed value - switching dynamic state can re-enter Scheme and cause GC
		alloc::StrongRef<AnyCell> passedValueRef(world, cont->m_passedValue);

		// Switch our dynamic state
		State::switchState(world, cont->m_dynamicState);

		return {
			.continuation = cont,
			.passedValue = passedValueRef
		};
	}
	else
	{
		// Unexpected return value from longjmp 
		abort();
	}
}
	
void Continuation::resume(World &world, AnyCell *passedValue)
{
	int currentStackCanary;
	void *stackPointer = &currentStackCanary;
	
	ptrdiff_t currentStackSize = static_cast<char*>(world.continuationBase) - static_cast<char*>(stackPointer);
	assert(currentStackSize > -1);

	if (currentStackSize < m_savedStackBytes)
	{
		// We need to allocate some more stack space otherwise we'll overwrite our own stack while executing
		// This means memcpy() will have its return stack corrupted and all sorts of badness will happen. Instead just
		// alloca() enough space and re-enter ourselves to ensure our entire stack frame is safe
		alloca(m_savedStackBytes - currentStackSize);
		resume(world, passedValue);
	}

	// Track that we're the resuming continuation
	world.resumingContinuation = this;

	// Stash our passed value inside the continuation
	m_passedValue = passedValue;

	// Copy the stack back over
	memcpy(static_cast<char*>(world.continuationBase) - m_savedStackBytes, m_savedStack, m_savedStackBytes); 

	// DO NOT PLACE CODE HERE - THE STACK IS IN AN INCONSISTENT STATE

	// Now jump back to the original location
	longjmp(m_jumpTarget, ContinuationResumeCookie);
}

}
}