#ifndef _LLIBY_BINDING_PROPERLIST_H
#define _LLIBY_BINDING_PROPERLIST_H

#include "ListElementCell.h"
#include "EmptyListCell.h"
#include "PairCell.h"

#include "alloc/RangeAlloc.h"
#include "alloc/StrongRef.h"

#include <iterator>

namespace lliby
{
	/**
	 * Represents the head of a proper list
	 *
	 * Proper lists are defined by Scheme to be a pair with a cdr of either a the empty list or another proper list.
	 * On the Scheme side they're defined with the type (Listof <type>) where <type> is the type of the car values.
	 * This is the analogous C++ representation. It supports forward iteration, size calculations and construction of
	 * new instances via ::create.
	 *
	 * This implements ::isInstance() which allows cell_cast<> and cell_unchecked_cast<> to convert other cells to
	 * the appropriate ProperList type.
	 */
	template<class T>
	class ProperList : public ListElementCell
	{
	public:
		using size_type = std::uint32_t;

		class Iterator : public std::iterator<std::forward_iterator_tag, T*>
		{
			friend class ProperList;
		public:
			T* operator*() const
			{
				auto pairHead = cell_unchecked_cast<const PairCell>(m_head);
				return cell_unchecked_cast<T>(pairHead->car());
			}

			bool operator==(const Iterator &other) const
			{
				return m_head == other.m_head;
			}

			bool operator!=(const Iterator &other) const
			{
				return m_head != other.m_head;
			}

			Iterator& operator++()
			{
				auto pairHead = cell_unchecked_cast<const PairCell>(m_head);
				m_head = cell_unchecked_cast<const ListElementCell>(pairHead->cdr());

				return *this;
			}

			Iterator operator++(int postfix)
			{
				Iterator originalValue(*this);
				++(*this);
				return originalValue;
			}

		private:
			explicit Iterator(const ListElementCell *head) :
				m_head(head)
			{
			}

			const ListElementCell *m_head;
		};

		/**
		 * Returns an iterator pointing to the beginning of this proper list
		 */
		typename ProperList<T>::Iterator begin() const
		{
			return typename ProperList<T>::Iterator(this);
		}

		typename ProperList<T>::Iterator end() const
		{
			return typename ProperList<T>::Iterator(EmptyListCell::instance());
		}

		/**
		 * Returns true if this proper list is empty
		 *
		 * This is more efficient than size() == 0
		 */
		bool empty() const
		{
			return begin() == end();
		}

		/**
		 * Returns this size of this proper list
		 *
		 * If the compiler produced a length hint then this is an O(1) operation. Otherwise it's O(n) with the length
		 * of the list. For that reason the length should be cached whenever possible.
		 */
		size_type size() const
		{
			// Try a length hint first
			if (auto pair = cell_cast<PairCell>(this))
			{
				if (pair->listLength() != 0)
				{
					return pair->listLength();
				}
			}

			// Calculate it manually
			return std::distance(begin(), end());
		}

		/**
		 * Creates a new ProperList instance containing the passed elements
		 *
		 * This requires entering the garbage collector. For that reason the elements vector is GC rooted before
		 * allocating the list elements.
		 */
		static ProperList<T> *create(World &world, std::vector<T*> &elements)
		{
			// Avoid GC rooting etc. if we don't need to allocate anything
			if (elements.empty())
			{
				return EmptyListCell::asProperList<T>();
			}

			// We allocate space for our pairs below. Make sure we GC root the new elements first.
			alloc::StrongRefRange<T> elementsRoot(world, elements);

			alloc::RangeAlloc allocation = alloc::allocateRange(world, elements.size());
			auto allocIt = allocation.end();

			auto it = elements.rbegin();
			AnyCell *cdr = EmptyListCell::instance();

			for(;it != elements.rend(); it++)
			{
				cdr = new (*--allocIt) PairCell(*it, cdr);
			}

			return static_cast<ProperList<T>*>(cdr);
		}

		static ProperList<T> *create(World &world, std::initializer_list<T*> elementsList)
		{
			std::vector<T*> elements(elementsList);
			return create(world, elements);
		}

		static ProperList<T> *create(World &world, const std::vector<T*> &elements)
		{
			std::vector<T*> elementsCopy(elements);
			return create(world, elementsCopy);
		}

		/**
		 * Returns true if the passed cell is a proper list of the correct type
		 *
		 * This is used to implement cell_cast<> for ProperList instances.
		 */
		static bool isInstance(const AnyCell *cell)
		{
			while(auto pair = cell_cast<PairCell>(cell))
			{
				if (!T::isInstance(pair->car()))
				{
					return false;
				}

				cell = pair->cdr();
			}

			if (cell != EmptyListCell::instance())
			{
				return false;
			}

			return true;
		}
	};
}

#endif
