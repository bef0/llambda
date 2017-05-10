#include "actor/ActorClosureCell.h"

#include <thread>
#include <chrono>

#include "binding/MailboxCell.h"
#include "binding/UnitCell.h"
#include "binding/TypedProcedureCell.h"

#include "actor/PoisonPillCell.h"
#include "actor/ActorContext.h"
#include "actor/ActorBehaviourCell.h"
#include "actor/SupervisorStrategyCell.h"
#include "actor/Mailbox.h"
#include "actor/Message.h"
#include "actor/Runner.h"
#include "actor/cloneCell.h"

#include "sched/TimerList.h"

#include "core/error.h"
#include "core/World.h"

using namespace lliby;

namespace
{
	actor::Message *createTellMessage(World &world, const char *procName, AnyCell *messageCell)
	{
		std::shared_ptr<actor::Mailbox> senderMailbox;
		actor::ActorContext *context = world.actorContext();

		// If we don't have an actor context leave the sender mailbox unset. This will eat all messages and return #f
		// for (mailbox-open?)
		if (context)
		{
			senderMailbox = context->mailbox();
		}

		try
		{
			actor::Message *msg = actor::Message::createFromCell(messageCell, senderMailbox);
			return msg;
		}
		catch(actor::UnclonableCellException &e)
		{
			e.signalSchemeError(world, procName);
		}
	}
}

extern "C"
{

using ReceiveProc = TypedProcedureCell<void, AnyCell*>;

MailboxCell* llactor_act(World &world, actor::ActorClosureCell *closureProc)
{
	try
	{
		return MailboxCell::createInstance(world, actor::Runner::start(world, closureProc));
	}
	catch(actor::UnclonableCellException &e)
	{
		e.signalSchemeError(world, "(act)");
	}
}

void llactor_tell(World &world, MailboxCell *destMailboxCell, AnyCell *messageCell)
{
	std::shared_ptr<actor::Mailbox> destMailbox(destMailboxCell->lockedMailbox());

	if (destMailbox)
	{
		destMailbox->tell(createTellMessage(world, "(tell)", messageCell));
	}
}

void llactor_schedule_once(World &world, std::int64_t delayUsecs, MailboxCell *destMailboxCell, AnyCell *messageCell)
{
	std::weak_ptr<actor::Mailbox> mailboxRef = destMailboxCell->mailboxRef();

	if (mailboxRef.expired())
	{
		// Already expired; skip the enqueue
		return;
	}

	const std::chrono::microseconds delay(delayUsecs);
	actor::Message *msg = createTellMessage(world, "(schedule-once)", messageCell);

	auto workFunction = [=] ()
	{
		std::shared_ptr<actor::Mailbox> destMailbox = mailboxRef.lock();

		if (!destMailbox)
		{
			// Expired while we were sleeping
			delete msg;
			return;
		}

		destMailbox->tell(msg);
	};

	sched::TimerList::defaultInstance().enqueueDelayedWork(workFunction, delay);
}

void llactor_forward(World &world, MailboxCell *destMailboxCell, AnyCell *messageCell)
{
	std::shared_ptr<actor::Mailbox> destMailbox(destMailboxCell->mailbox().lock());

	if (!destMailbox)
	{
		// Destination has gone away
		return;
	}

	actor::ActorContext *context = world.actorContext();

	if (context == nullptr)
	{
		signalError(world, ErrorCategory::NoActor, "Attempted (forward) outside actor context");
	}

	try
	{
		actor::Message *msg = actor::Message::createFromCell(messageCell, context->sender());
		destMailbox->tell(msg);
	}
	catch(actor::UnclonableCellException &e)
	{
		e.signalSchemeError(world, "(forward)");
	}
}

AnyCell* llactor_ask(World &world, MailboxCell *destMailboxCell, AnyCell *messageCell, std::int64_t timeoutUsecs)
{
	std::shared_ptr<actor::Mailbox> destMailbox(destMailboxCell->lockedMailbox());

	if (!destMailbox)
	{
		signalError(world, ErrorCategory::AskTimeout, "(ask) on closed mailbox");
	}
	else
	{
		try
		{
			AnyCell *result = destMailbox->ask(world, messageCell, timeoutUsecs);

			if (result == nullptr)
			{
				signalError(world, ErrorCategory::AskTimeout, "(ask) timeout");
			}

			return result;
		}
		catch(actor::UnclonableCellException &e)
		{
			e.signalSchemeError(world, "(ask)");
		}
	}
}

MailboxCell *llactor_self(World &world)
{
	actor::ActorContext *context = world.actorContext();

	if (context == nullptr)
	{
		signalError(world, ErrorCategory::NoActor, "Attempted (self) outside actor context");
	}

	return MailboxCell::createInstance(world, context->mailbox());
}

AnyCell *llactor_sender(World &world)
{
	actor::ActorContext *context = world.actorContext();

	if (context == nullptr)
	{
		signalError(world, ErrorCategory::NoActor, "Attempted (sender) outside actor context");
	}

	std::shared_ptr<actor::Mailbox> mailbox = context->sender().lock();

	if (!mailbox)
	{
		// No last sender
		return UnitCell::instance();
	}

	return MailboxCell::createInstance(world, mailbox);
}

void llactor_stop(MailboxCell *mailboxCell)
{
	std::shared_ptr<actor::Mailbox> mailbox(mailboxCell->lockedMailbox());

	if (mailbox)
	{
		mailbox->requestLifecycleAction(actor::LifecycleAction::Stop);
	}
}

bool llactor_graceful_stop(MailboxCell *mailboxCell)
{
	std::shared_ptr<actor::Mailbox> mailbox(mailboxCell->lockedMailbox());

	// If there's no mailbox we're already stopped
	if (mailbox)
	{
		mailbox->requestLifecycleAction(actor::LifecycleAction::Stop);
		mailbox->waitForStop();
	}

	// XXX: Timeout support
	return true;
}

bool llactor_mailbox_is_open(World &world, MailboxCell *mailboxCell)
{
	return !mailboxCell->mailbox().expired();
}

actor::PoisonPillCell* llactor_poison_pill_object()
{
	return actor::PoisonPillCell::instance();
}

bool llactor_is_poison_pill_object(AnyCell *cell)
{
	return actor::PoisonPillCell::instance() == cell;
}

void llactor_become(World &world, actor::ActorBehaviourCell *newBehaviour)
{
	actor::ActorContext *context = world.actorContext();

	if (context == nullptr)
	{
		signalError(world, ErrorCategory::NoActor, "Attempted (become) outside actor context");
	}

	context->setBehaviour(newBehaviour);
}

void llactor_set_supervisor_strategy(World &world, actor::SupervisorStrategyCell *strategy)
{
	actor::ActorContext *context = world.actorContext();

	if (context == nullptr)
	{
		signalError(world, ErrorCategory::NoActor, "Attempted (set-supervisor-strategy) outside actor context");
	}

	context->setSupervisorStrategy(strategy);
}

}
