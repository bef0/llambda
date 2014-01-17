#ifndef _LLIBY_BINDING_UNITCELL_H
#define _LLIBY_BINDING_UNITCELL_H

#include "PreconstructedValue.h"
#include "core/constinstances.h"

namespace lliby
{

class UnitCell : public PreconstructedValue<DatumCell>
{
#include "generated/UnitCellMembers.h"
public:
	UnitCell() :
		PreconstructedValue(CellTypeId::Unit)
	{
	}
	
	static const UnitCell* instance()
	{
		return &lliby_unit_value;
	}
};

}

#endif