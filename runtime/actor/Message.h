#ifndef _LLIBY_ACTOR_MESSAGE_H
#define _LLIBY_ACTOR_MESSAGE_H

#include "binding/AnyCell.h"
#include "alloc/Heap.h"

#include <memory>

namespace lliby
{
namespace actor
{

class Mailbox;

/**
 * Message for a Mailbox
 *
 * This contains a messageCell and a heap. The messageCell is the message actually being sent. If the message cell is
 * not a constant a non-empty heap will also be passed containing the message cell and its children.
 */
class Message
{
public:
	/**
	 * Creates a new message from the passed cell
	 *
	 * This clones the message cell in to a new heap and returns the resulting message. If an unclonable cell is
	 * encountered then an UnclonableCellException will be thrown.
	 *
	 * @param  cell    Cell to copy in to the message
	 * @parem  sender  Mailbox of the sender
	 */
	static Message *createFromCell(AnyCell *cell, const std::shared_ptr<Mailbox> &sender);

	AnyCell* messageCell() const
	{
		return m_messageCell;
	}

	alloc::Heap& heap()
	{
		return m_heap;
	}

	std::weak_ptr<Mailbox> sender() const
	{
		return m_sender;
	}

private:
	Message()
	{
	}

	AnyCell *m_messageCell;
	alloc::Heap m_heap;
	std::weak_ptr<Mailbox> m_sender;
};

}
}

#endif
