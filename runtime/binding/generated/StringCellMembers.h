/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	std::uint16_t allocSlackBytes() const
	{
		return m_allocSlackBytes;
	}

	std::uint32_t charLength() const
	{
		return m_charLength;
	}

	std::uint32_t byteLength() const
	{
		return m_byteLength;
	}

public:
	static bool typeIdIsTypeOrSubtype(CellTypeId typeId)
	{
		return typeId == CellTypeId::String;
	}

	static bool isInstance(const DatumCell *datum)
	{
		return typeIdIsTypeOrSubtype(datum->typeId());
	}

private:
	std::uint16_t m_allocSlackBytes;
	std::uint32_t m_charLength;
	std::uint32_t m_byteLength;
