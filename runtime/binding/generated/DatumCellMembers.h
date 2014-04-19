/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	CellTypeId typeId() const
	{
		return m_typeId;
	}

	GarbageState gcState() const
	{
		return m_gcState;
	}

public:
	static bool typeIdIsTypeOrSubtype(CellTypeId typeId)
	{
		return true;
	}

	static bool isInstance(const DatumCell *datum)
	{
		return typeIdIsTypeOrSubtype(datum->typeId());
	}

private:
	CellTypeId m_typeId;
	GarbageState m_gcState;
