#include "StringValue.h"

#include <string.h>
#include <vector>
#include <algorithm>

#include "SymbolValue.h"
#include "ByteVectorValue.h"

namespace
{
	using namespace lliby;

	const std::uint8_t ContinuationHeaderMask  = 0xC0;
	const std::uint8_t ContinuationHeaderValue = 0x80;

	const std::uint8_t TwoByteHeaderMask  = 0xE0;
	const std::uint8_t TwoByteHeaderValue = 0xC0;
	
	const std::uint8_t ThreeByteHeaderMask  = 0xF0;
	const std::uint8_t ThreeByteHeaderValue = 0xE0;
	
	const std::uint8_t FourByteHeaderMask  = 0xF8;
	const std::uint8_t FourByteHeaderValue = 0xF0;
	
	bool isContinuationByte(std::uint8_t byte)
	{
		// Any UTF-8 byte in the form 10xxxxxx is a continuation byte
		return (byte & ContinuationHeaderMask) == ContinuationHeaderValue;
	}

	StringValue::CodePoint decodeUtf8Char(const std::uint8_t *charPtr, unsigned int *length = nullptr)
	{
		unsigned int continuationBytes;
		StringValue::CodePoint codePoint;

		std::uint8_t firstByte = *charPtr;

		if (firstByte <= 0x7f)
		{
			// This is ASCII, it's easy
			if (length)
			{
				*length = 1;
			}

			return firstByte;
		}

		if ((firstByte & TwoByteHeaderMask) == TwoByteHeaderValue)
		{
			continuationBytes = 1;
			codePoint = firstByte & ~TwoByteHeaderMask;
		}
		else if ((firstByte & ThreeByteHeaderMask) == ThreeByteHeaderValue)
		{
			continuationBytes = 2;
			codePoint = firstByte & ~ThreeByteHeaderMask;
		}
		else if ((firstByte & FourByteHeaderMask) == FourByteHeaderValue)
		{
			continuationBytes = 3;
			codePoint = firstByte & ~FourByteHeaderMask;
		}
		else
		{
			return StringValue::InvalidChar;
		}

		if (length)
		{
			*length = continuationBytes + 1;
		}

		while(continuationBytes--)
		{
			charPtr++;

			// This will also catch running off the end via NULL
			if (!isContinuationByte(*charPtr))
			{
				return StringValue::InvalidChar;
			}

			codePoint = (codePoint << 6) | (*charPtr & ~ContinuationHeaderMask);
		}

		return codePoint;
	}

	std::vector<std::uint8_t> encodeUtf8Char(StringValue::CodePoint codePoint)
	{
		unsigned int continuationBytes;
		std::uint8_t firstByteHeader;

		if (codePoint > 0x10FFFF)
		{
			// Not valid - see RFC-3629
			return std::vector<std::uint8_t>();
		}
		else if (codePoint >= 0x10000)
		{
			continuationBytes = 3;
			firstByteHeader = FourByteHeaderValue;
		}
		else if (codePoint >= 0x800)
		{
			continuationBytes = 2;
			firstByteHeader = ThreeByteHeaderValue;
		}
		else if (codePoint >= 0x80)
		{
			continuationBytes = 1;
			firstByteHeader = TwoByteHeaderValue;
		}
		else
		{
			// Pure ASCII
			return std::vector<std::uint8_t>{static_cast<std::uint8_t>(codePoint)};
		}

		std::vector<std::uint8_t> encoded(continuationBytes + 1);

		for(unsigned int i = continuationBytes; i; i--)
		{
			encoded[i] = ContinuationHeaderValue | (~ContinuationHeaderMask & codePoint);
			codePoint = codePoint >> 6;
		}

		encoded[0] = firstByteHeader | codePoint;

		return encoded;
	}
}

namespace lliby
{

StringValue* StringValue::fromUtf8CString(const char *signedStr)
{
	std::uint64_t byteLength = 0;
	std::uint32_t charLength = 0;

	auto *str = reinterpret_cast<const std::uint8_t*>(signedStr);

	// Check for length in bytes and non-ASCII characters
	for(auto scanPtr = str;; scanPtr++)
	{
		if (*scanPtr == 0)
		{
			// We've reached the end of the string
			break;
		}

		if (!isContinuationByte(*scanPtr))
		{
			charLength++;
		}

		byteLength++;
	}

	if (byteLength > std::numeric_limits<std::uint32_t>::max())
	{
		return nullptr;
	}

	// Allocate the new string room for the NULL terminator
	auto *newString = new std::uint8_t[byteLength + 1];
	memcpy(newString, str, byteLength + 1);

	return new StringValue(newString, byteLength, charLength);
}
	
StringValue* StringValue::fromUtf8Data(const std::uint8_t *data, std::uint32_t byteLength)
{
	std::uint32_t charLength = 0;
	auto newString = new std::uint8_t[byteLength + 1];

	// It seems that the cache friendliness of doing this in one loop likely
	// outweighs the faster copy we'd get from doing memcpy() at the end
	for(std::uint32_t i = 0; i < byteLength; i++)
	{
		if (!isContinuationByte(data[i]))
		{
			charLength++;
		}

		newString[i] = data[i];
	}

	newString[byteLength] = 0;

	return new StringValue(newString, byteLength, charLength);
}
	
StringValue* StringValue::fromFill(std::uint32_t length, CodePoint fill)
{
	// Figure out how many bytes we'll need
	std::vector<std::uint8_t> encoded = encodeUtf8Char(fill);
	const size_t encodedCharSize = encoded.size();

	const std::uint32_t byteLength = encodedCharSize * length;

	// Allocate the string
	auto newString = new std::uint8_t[byteLength + 1];

	// Actually fill
	for(std::uint32_t i = 0; i < length; i++) 
	{
		memcpy(&newString[i * encodedCharSize], encoded.data(), encodedCharSize);
	}

	// NULL terminate
	newString[encodedCharSize * length] = 0;

	return new StringValue(newString, byteLength, length);
}
	
StringValue* StringValue::fromAppended(const std::list<const StringValue*> &strings)
{
	std::uint64_t totalByteLength = 0;
	std::uint32_t totalCharLength = 0;

	for(auto stringPart : strings)
	{
		totalByteLength += stringPart->byteLength();
		totalCharLength += stringPart->charLength();
	}

	// We only have to check bytelength because charLength must always be <=
	if (totalByteLength > std::numeric_limits<std::uint32_t>::max())
	{
		return nullptr;
	}

	// Allocate the new string and null terminate it
	auto newString = new std::uint8_t[totalByteLength + 1];
	newString[totalByteLength] = 0;

	// Copy all the string parts over
	auto copyPtr = newString;
	
	for(auto stringPart : strings)
	{
		memcpy(copyPtr, stringPart->utf8Data(), stringPart->byteLength());
		copyPtr += stringPart->byteLength();
	}

	return new StringValue(newString, totalByteLength, totalCharLength);
}
	
StringValue* StringValue::fromCodePoints(const std::list<CodePoint> &codePoints)
{
	std::vector<std::uint8_t> encodedData;
	std::uint32_t charLength = 0;

	// The encoded data will have the be at least this size
	encodedData.reserve(codePoints.size());

	for(auto codePoint : codePoints)
	{
		const std::vector<std::uint8_t> encodedChar = encodeUtf8Char(codePoint);
		encodedData.insert(encodedData.end(), encodedChar.begin(), encodedChar.end());

		charLength++;
	}
	
	const std::uint32_t totalByteLength = encodedData.size();
	auto newString = new std::uint8_t[totalByteLength + 1];
	memcpy(newString, encodedData.data(), totalByteLength);

	newString[totalByteLength] = 0;

	return new StringValue(newString, totalByteLength, charLength);
}
	
std::uint8_t* StringValue::charPointer(std::uint32_t charOffset) const
{
	return charPointer(utf8Data(), byteLength(), charOffset);
}

std::uint8_t* StringValue::charPointer(std::uint8_t *scanFrom, std::uint32_t bytesLeft, uint32_t charOffset) const
{
	if (asciiOnly())
	{
		if (charOffset >= bytesLeft)
		{
			return nullptr;
		}

		return &scanFrom[charOffset];
	}

	while(bytesLeft > 0)
	{
		if (!isContinuationByte(*scanFrom))
		{
			if (charOffset == 0)
			{
				return scanFrom;
			}
			
			charOffset--;
		}

		bytesLeft--;
		scanFrom++;
	}

	return nullptr;
}
	
StringValue::CharRange StringValue::charRange(std::int64_t start, std::int64_t end) const
{
	if (end != -1)
	{
		if ((end < start) || (end > charLength()))
		{
			// Doesn't make sense
			return CharRange { 0 };
		}
	}

	// We need the extra byte to allow a zero length range after the last char
	// in the string 
	std::uint8_t *startPointer = charPointer(utf8Data(), byteLength() + 1, start);

	if (startPointer == nullptr)
	{
		// Fell off the end
		return CharRange { 0 };
	}

	std::uint32_t charCount;
	std::uint8_t *endPointer;
	
	if (end != -1)
	{
		charCount = end - start;
		
		// We have a NULL at the end of our data that's not included in length.
		// Allow for that extra byte so that charPointer can return the end of 
		// the string.
		const std::uint32_t bytesLeft = byteLength() - (startPointer - utf8Data()) + 1;

		// Find our end pointer
		endPointer = charPointer(startPointer, bytesLeft, charCount);

		if (endPointer == nullptr)
		{
			// Fell off the end
			return CharRange { 0 };
		}
	}
	else 
	{
		charCount = charLength() - start;
		endPointer = &utf8Data()[byteLength()];
	}

	return CharRange { startPointer, endPointer, charCount };
}
	
StringValue::CodePoint StringValue::charAt(std::uint32_t offset) const
{
	std::uint8_t* charPtr = charPointer(offset);

	if (charPtr == nullptr)
	{
		return InvalidChar;
	}

	return decodeUtf8Char(charPtr);
}
	
bool StringValue::replaceBytes(const CharRange &range, std::uint8_t *pattern, unsigned int patternBytes, unsigned int count)
{
	const unsigned int requiredBytes = patternBytes * count;
	const unsigned int replacedBytes = range.byteCount();
	
	if (requiredBytes == replacedBytes)
	{
		std::uint8_t *copyDest = range.startPointer;
		while(count--)
		{
			memcpy(copyDest, pattern, patternBytes);
			copyDest += patternBytes;
		}
	}
	else
	{
		// Create a new string from pieces of the old string
		const std::uint64_t newByteLength = byteLength() + requiredBytes - replacedBytes;
		
		if (newByteLength > std::numeric_limits<std::uint32_t>::max())
		{
			return false;
		}

		const std::uint32_t initialBytes = range.startPointer - utf8Data(); 
		// Include the NULL terminator
		const std::uint32_t finalBytes = newByteLength + 1 - initialBytes - requiredBytes;
		
		const bool needRealloc = newByteLength > byteLength();

		std::uint8_t* destString;
		
		if (needRealloc)
		{
			destString = new std::uint8_t[newByteLength + 1];
			// Fill the initial chunk of the string
			memcpy(destString, utf8Data(), initialBytes); 
		}
		else
		{
			destString = utf8Data();
			// The initial chunk is already correct
		}
		
		std::uint8_t* copyDest = destString + initialBytes;

		while(count--)
		{
			// memmove because ::replace() can copy a substring to itself
			memmove(copyDest, pattern, patternBytes);
			copyDest += patternBytes;
		}

		memmove(copyDest, range.startPointer + replacedBytes, finalBytes);

		// Update ourselves with our new string
		setByteLength(newByteLength);

		if (needRealloc)
		{
			delete[] utf8Data();
			setUtf8Data(destString);
		}
	}
	
	return true;
}
	
bool StringValue::fill(CodePoint codePoint, std::int64_t start, std::int64_t end)
{
	CharRange range = charRange(start, end);

	if (range.isNull())
	{
		// Invalid range
		return false;
	}

	// Encode the new character
	std::vector<std::uint8_t> encoded = encodeUtf8Char(codePoint);

	if (encoded.size() == 0)
	{
		// Invalid code point
		return false;
	}

	return replaceBytes(range, encoded.data(), encoded.size(), range.charCount);
}
	
bool StringValue::replace(std::uint32_t offset, const StringValue *from, std::int64_t fromStart, std::int64_t fromEnd)
{
	CharRange fromRange = from->charRange(fromStart, fromEnd);

	if (fromRange.isNull())
	{
		return false;
	}

	CharRange toRange = charRange(offset, offset + fromRange.charCount);

	if (toRange.isNull() || (toRange.charCount != fromRange.charCount))
	{
		return false;
	}
	
	return replaceBytes(toRange, fromRange.startPointer, fromRange.byteCount());
}
	
bool StringValue::setCharAt(std::uint32_t offset, CodePoint codePoint)
{
	return fill(codePoint, offset, offset + 1);
}
	
StringValue* StringValue::copy(std::int64_t start, std::int64_t end)
{
	CharRange range = charRange(start, end);

	if (range.isNull())
	{
		// Invalid range
		return nullptr;
	}

	const std::uint32_t newByteLength = range.byteCount();

	// Create the new string
	auto newString = new std::uint8_t[newByteLength + 1];
	memcpy(newString, range.startPointer, newByteLength);
	newString[newByteLength] = 0;

	return new StringValue(newString, newByteLength, range.charCount);
}
	
std::list<StringValue::CodePoint> StringValue::codePoints(std::int64_t start, std::int64_t end) const
{
	if ((end != -1) && (end < start))
	{
		// Doesn't make sense
		return std::list<CodePoint>();
	}

	std::uint8_t *scanPtr = charPointer(start);
	std::uint8_t *endPtr = &utf8Data()[byteLength()];

	if (scanPtr == nullptr)
	{
		return std::list<CodePoint>();
	}

	// It doesn't look like std::list::size() is O(1)
	unsigned int addedChars = 0;
	std::list<CodePoint> ret;

	while(scanPtr < endPtr)
	{
		unsigned int charLength;

		CodePoint codePoint = decodeUtf8Char(scanPtr, &charLength);

		if (codePoint == InvalidChar)
		{
			return std::list<CodePoint>();
		}

		ret.push_back(codePoint);

		scanPtr += charLength;
		addedChars++;

		if ((end != -1) && (addedChars == (end - start)))
		{
			// We have enough characters
			return ret;
		}
	}
		
	if (end != -1)
	{
		// We fell off the end
		return std::list<CodePoint>();
	}

	return ret;
}

int StringValue::compare(const StringValue *other) const
{
	std::uint32_t compareBytes = std::min(byteLength(), other->byteLength());

	// Bytewise comparisons in UTF-8 sort Unicode code points correctly
	int result = memcmp(utf8Data(), other->utf8Data(), compareBytes);

	if (result != 0)
	{
		return result;
	}
	else if (byteLength() > other->byteLength())
	{
		return 1;
	}
	else if (byteLength() < other->byteLength())
	{
		return -1;
	}
	else
	{
		return 0;
	}
}
	
SymbolValue* StringValue::toSymbol() const
{
	// This is easy, just copy our UTF-8 data
	auto newString = new std::uint8_t[byteLength() + 1];
	memcpy(newString, utf8Data(), byteLength() + 1);

	return new SymbolValue(newString, byteLength(), charLength());
}
	
ByteVectorValue* StringValue::toUtf8ByteVector(std::int64_t start, std::int64_t end) const
{
	CharRange range = charRange(start, end);

	if (range.isNull())
	{
		return nullptr;
	}

	std::uint32_t newLength = range.endPointer - range.startPointer;
	auto *newData = new std::uint8_t[newLength];

	memcpy(newData, range.startPointer, newLength);
	return new ByteVectorValue(newData, newLength);
}

}