#ifndef _LLIBY_BINDING_STRINGVALUE_H
#define _LLIBY_BINDING_STRINGVALUE_H

#include "StringLikeValue.h"
#include <list>

namespace lliby
{

class StringValue : public StringLikeValue
{
#include "generated/StringValueMembers.h"
public:
	StringValue(std::uint8_t *utf8Data, std::uint32_t byteLength, std::uint32_t charLength) :
		StringLikeValue(BoxedTypeId::String, utf8Data, byteLength, charLength)
	{
	}

	typedef std::int32_t CodePoint;
	static const CodePoint InvalidChar = -1;

	static StringValue* fromUtf8CString(const char *str);
	static StringValue* fromUtf8Data(const std::uint8_t *data, std::uint32_t byteLength);
	static StringValue* fromFill(std::uint32_t length, CodePoint fill);
	static StringValue* fromAppended(const std::list<const StringValue*> &strings);
	static StringValue* fromCodePoints(const std::list<CodePoint> &codePoints);

	StringValue* copy(std::int64_t start = 0, std::int64_t end = -1); 

	CodePoint charAt(std::uint32_t offset) const;
	bool setCharAt(std::uint32_t offset, CodePoint codePoint);

	bool fill(CodePoint codePoint, std::int64_t start = 0, std::int64_t end = -1);
	bool replace(std::uint32_t offset, const StringValue *from, std::int64_t fromStart = 0, std::int64_t fromEnd = -1);

	std::list<CodePoint> codePoints(std::int64_t start = 0, std::int64_t end = -1) const;

	bool operator==(const StringValue &other) const
	{
		return equals(other);
	}
	
	bool operator!=(const StringValue &other) const
	{
		return !equals(other);
	}

	// Returns and integer less than, equal to or greater than zero if the string
	// less than, equal to or greater than the other string
	int compare(const StringValue *other) const;

	bool asciiOnly() const
	{
		return byteLength() == charLength();
	}

	SymbolValue *toSymbol() const;
	ByteVectorValue *toUtf8ByteVector(std::int64_t start = 0, std::int64_t end = -1) const;

private:
	std::uint8_t *charPointer(std::uint8_t *scanFrom, std::uint32_t bytesLeft, uint32_t charOffset) const;
	std::uint8_t *charPointer(std::uint32_t charOffset) const;

	struct CharRange
	{
		std::uint8_t *startPointer;
		std::uint8_t *endPointer;
		std::uint32_t charCount;

		bool isNull() const
		{
			return startPointer == nullptr;
		};

		unsigned int byteCount() const
		{
			return endPointer - startPointer;
		}
	};

	CharRange charRange(std::int64_t start, std::int64_t end = -1) const; 
	bool replaceBytes(const CharRange &range, std::uint8_t *pattern, unsigned int patternBytes, unsigned int count = 1);
};

}

#endif

