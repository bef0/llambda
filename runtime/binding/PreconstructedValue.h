#ifndef _LLIBY_BINDING_PRECONSTRUCTEDVALUE_H
#define _LLIBY_BINDING_PRECONSTRUCTEDVALUE_H

#include "AnyCell.h"

namespace lliby
{

template <class T>
class PreconstructedValue : public T
{
public:
	explicit PreconstructedValue(CellTypeId typeId) :
		// Don't attempt to collect this as garbage
		T(typeId, GarbageState::GlobalConstant)
	{
	}

	// Disallow heap creation to force singleton use
	void *operator new(std::size_t size) = delete;
};

}

#endif
