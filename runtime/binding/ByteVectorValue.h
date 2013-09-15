#ifndef _LLIBY_BINDING_BYTEVECTORVALUE_H
#define _LLIBY_BINDING_BYTEVECTORVALUE_H

#include "BoxedDatum.h"
#include <list>

namespace lliby
{

class ByteVectorValue : public BoxedDatum
{
#include "generated/ByteVectorValueMembers.h"
public:
	ByteVectorValue(std::uint8_t *data, std::uint32_t length) :
		BoxedDatum(BoxedTypeId::ByteVector),
		m_length(length),
		m_data(data)
	{
	}

	static const std::int16_t InvalidByte = -1;

	void finalize();

	static ByteVectorValue* fromFill(std::uint32_t length, std::uint8_t fill = 0);
	static ByteVectorValue* fromAppended(const std::list<const ByteVectorValue*> &byteVectors);

	ByteVectorValue* copy(std::int64_t start = 0, std::int64_t end = -1); 
	bool replace(std::uint32_t offset, const ByteVectorValue *from, std::int64_t fromStart = 0, std::int64_t fromEnd = -1);

	StringValue* utf8ToString(std::int64_t start = 0, std::int64_t end = -1);

	std::int16_t byteAt(std::uint32_t offset) const
	{
		if (offset >= length())
		{
			return -1;
		}
		
		return data()[offset];
	}

	bool setByteAt(std::uint32_t offset, std::uint8_t value)
	{
		if (offset >= length())
		{
			return false;
		}

		data()[offset] = value;

		return true;
	}
};

}

#endif

