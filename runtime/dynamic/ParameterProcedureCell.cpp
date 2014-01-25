#include "dynamic/ParameterProcedureCell.h"

#include "dynamic/State.h"
#include "binding/EmptyListCell.h"
#include "core/fatal.h"

namespace lliby
{
namespace dynamic
{

namespace
{
	// This value should blow up the GC as a sanity check that we registered our class with registerRecordClass() at
	// startup
	std::uint32_t registeredClassId = ~0;

	DatumCell *procedureBody(ProcedureCell *self, ListElementCell *argHead)
	{
		if (argHead != EmptyListCell::instance())
		{
			_lliby_fatal("Parameter procedures don't accept arguments", argHead);
		}

		// We know we're a parameter procedure because only parameter procedures have us as an entry point
		auto parameterProc = static_cast<ParameterProcedureCell*>(self);
		return State::activeState()->valueForParameter(parameterProc);
	}
}
	
ParameterProcedureCell::ParameterProcedureCell(DatumCell *initialValue, ProcedureCell *converterProcedure) :
	ProcedureCell(registeredClassId, false, nullptr, &procedureBody) 
{
	auto closure = static_cast<ParameterProcedureClosure*>(allocateRecordData(sizeof(ParameterProcedureClosure)));

	closure->initialValue = initialValue;

	if (converterProcedure == nullptr)
	{
		// No converter
		closure->converter = EmptyListCell::instance();
	}
	else
	{
		closure->converter = converterProcedure;
	}

	setRecordData(closure);
}

bool ParameterProcedureCell::isInstance(const ProcedureCell *proc)
{
	return proc->recordClassId() == registeredClassId;
}

void ParameterProcedureCell::registerRecordClass()
{
	// Register our closure type so our garbage collector knows what to do
	std::vector<size_t> offsets = {
		offsetof(ParameterProcedureClosure, initialValue),
		offsetof(ParameterProcedureClosure, converter)
	};

	registeredClassId = RecordLikeCell::registerRuntimeRecordClass(offsets);  
}

}
}
