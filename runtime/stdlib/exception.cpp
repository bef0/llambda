#include "binding/DatumCell.h"
#include "binding/ProcedureCell.h"
#include "binding/EmptyListCell.h"
#include "binding/ListElementCell.h"
#include "binding/ErrorObjectCell.h"

#include "alloc/StrongRef.h"
#include "dynamic/State.h"
#include "dynamic/SchemeException.h"

#include <iostream>

using namespace lliby;

extern "C"
{

DatumCell* lliby_with_exception_handler(World *world, ProcedureCell *handlerRaw, ProcedureCell *thunk)
{
	// Root our exception handler
	alloc::StrongRef<ProcedureCell> handler(handlerRaw);

	// Keep track of our dynamic state
	dynamic::State *expectedState = dynamic::State::activeState();

	try
	{
		return thunk->apply(world, EmptyListCell::instance());
	}
	catch (dynamic::SchemeException &except)
	{
		ListElementCell *argHead = ListElementCell::createProperList({except.object()});

		// Call the handler in the dynamic environment (raise) was in
		// This is required by R7RS for reasons mysterious to me
		handler->apply(world, argHead);
		
		// Now switch to the state we were in before re-raising the exception
		dynamic::State::switchState(world, expectedState);

		throw except;
	}
}

void lliby_raise(DatumCell *obj)
{
	throw dynamic::SchemeException(obj);
}

void lliby_error(StringCell *message, ListElementCell *irritants)
{
	lliby_raise(ErrorObjectCell::createInstance(message, irritants));
}

StringCell* lliby_error_object_message(ErrorObjectCell *errorObject)
{
	return errorObject->message();
}

ListElementCell* lliby_error_object_irritants(ErrorObjectCell *errorObject)
{
	return errorObject->irritants();
}

}
