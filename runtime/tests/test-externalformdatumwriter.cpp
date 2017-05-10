#include <string>
#include <sstream>

#include "core/init.h"
#include "core/World.h"

#include "writer/ExternalFormDatumWriter.h"
#include "binding/UnitCell.h"
#include "binding/EmptyListCell.h"
#include "binding/BooleanCell.h"
#include "binding/IntegerCell.h"
#include "binding/FlonumCell.h"
#include "binding/StringCell.h"
#include "binding/SymbolCell.h"
#include "binding/PairCell.h"
#include "binding/BytevectorCell.h"
#include "binding/VectorCell.h"
#include "binding/ProcedureCell.h"
#include "binding/CharCell.h"
#include "binding/RecordCell.h"
#include "binding/ErrorObjectCell.h"
#include "binding/PortCell.h"
#include "binding/EofObjectCell.h"
#include "binding/MailboxCell.h"
#include "binding/HashMapCell.h"

#include "hash/DatumHashTree.h"

#include "port/StandardOutputPort.h"
#include "actor/Mailbox.h"

#include "assertions.h"
#include "stubdefinitions.h"

namespace
{
using namespace lliby;

std::string externalFormFor(const AnyCell *datum)
{
	std::ostringstream outputStream;

	ExternalFormDatumWriter writer(outputStream);
	writer.render(datum);

	return outputStream.str();
}

void assertForm(const AnyCell *datum, std::string expected)
{
	ASSERT_EQUAL(externalFormFor(datum), expected);
}

SymbolCell *symbolFor(World &world, const char *utf8String)
{
	return SymbolCell::fromUtf8StdString(world, utf8String);
}

StringCell *stringFor(World &world, const char *utf8String)
{
	return StringCell::fromUtf8StdString(world, utf8String);
}

void testUnit()
{
	assertForm(UnitCell::instance(), "#!unit");
}

void testEmptyList()
{
	assertForm(EmptyListCell::instance(), "()");
}

void testBoolean()
{
	assertForm(BooleanCell::trueInstance(), "#t");
	assertForm(BooleanCell::falseInstance(), "#f");
}

void testInteger(World &world)
{
	assertForm(IntegerCell::fromValue(world, 25), "25");
	assertForm(IntegerCell::fromValue(world, 0), "0");
	assertForm(IntegerCell::fromValue(world,-31337), "-31337");
}

void testFlonum(World &world)
{
	assertForm(FlonumCell::fromValue(world, 0.0), "0.0");

	assertForm(FlonumCell::fromValue(world, 12.5), "12.5");
	assertForm(FlonumCell::fromValue(world, -4.5), "-4.5");

	assertForm(FlonumCell::fromValue(world, 100.0), "100.0");
	assertForm(FlonumCell::fromValue(world, -500.0), "-500.0");

	assertForm(FlonumCell::NaN(world), "+nan.0");
	assertForm(FlonumCell::positiveInfinity(world), "+inf.0");
	assertForm(FlonumCell::negativeInfinity(world), "-inf.0");
}

void testSymbol(World &world)
{
	assertForm(symbolFor(world, u8"Hello"), u8"Hello");
	assertForm(symbolFor(world, u8"HelloWorldThisRequiresHeapAllocation"), u8"HelloWorldThisRequiresHeapAllocation");
	assertForm(symbolFor(world, u8"λ"), u8"|λ|");
	assertForm(symbolFor(world, u8"Hello, world"), u8"|Hello, world|");
	assertForm(symbolFor(world, u8"Back\\slash"), u8"|Back\\\\slash|");
	assertForm(symbolFor(world, u8"P|pe"), u8"|P\\|pe|");
	assertForm(symbolFor(world, u8"Quo\"te"), u8"|Quo\"te|");
	assertForm(symbolFor(world, u8""), u8"||");
	assertForm(symbolFor(world, u8"Open[square"), u8"|Open[square|");
	assertForm(symbolFor(world, u8"Close]square"), u8"|Close]square|");
	assertForm(symbolFor(world, u8"Open(round"), u8"|Open(round|");
	assertForm(symbolFor(world, u8"Close)round"), u8"|Close)round|");
	assertForm(symbolFor(world, u8"Mid#hash"), u8"|Mid#hash|");
	assertForm(symbolFor(world, u8"Mid'quote"), u8"|Mid'quote|");
	assertForm(symbolFor(world, u8"Mid,comma"), u8"|Mid,comma|");
	assertForm(symbolFor(world, u8"Mid`backtick"), u8"|Mid`backtick|");

	// This is allowed as @ is only special after a ,
	assertForm(symbolFor(world, u8"Mid@at"), u8"Mid@at");

	// These are "peculiar identifiers"
	assertForm(symbolFor(world, "+"), u8"+");
	assertForm(symbolFor(world, "-"), u8"-");

	// (explicit sign) (sign subseqqent) (subsequent)*
	assertForm(symbolFor(world, "++"), u8"++");
	assertForm(symbolFor(world, "+@"), u8"+@");
	assertForm(symbolFor(world, "-+foo"), u8"-+foo");
	assertForm(symbolFor(world, "+@foo"), u8"+@foo");

	// (explicit sign) . (dot subsequent) (subsequent)*
	assertForm(symbolFor(world, "+.+"), u8"+.+");
	assertForm(symbolFor(world, "-.@"), u8"-.@");
	assertForm(symbolFor(world, "+.."), u8"+..");
	assertForm(symbolFor(world, "-.+foo"), u8"-.+foo");
	assertForm(symbolFor(world, "+.@foo"), u8"+.@foo");
	assertForm(symbolFor(world, "-..foo"), u8"-..foo");

	// . (dot subsequent) (subsequent)*
	assertForm(symbolFor(world, ".+"), u8".+");
	assertForm(symbolFor(world, ".@"), u8".@");
	assertForm(symbolFor(world, ".."), u8"..");
	assertForm(symbolFor(world, ".+foo"), u8".+foo");
	assertForm(symbolFor(world, ".@foo"), u8".@foo");
	assertForm(symbolFor(world, "..foo"), u8"..foo");

	// These are also numbers
	assertForm(symbolFor(world, u8"0"), u8"|0|");
	assertForm(symbolFor(world, u8"+0"), u8"|+0|");
	assertForm(symbolFor(world, u8"-0"), u8"|-0|");
	assertForm(symbolFor(world, u8"0.5"), u8"|0.5|");
	assertForm(symbolFor(world, u8"+0.5"), u8"|+0.5|");
	assertForm(symbolFor(world, u8"-0.5"), u8"|-0.5|");
	assertForm(symbolFor(world, u8"2/3"), u8"|2/3|");
	assertForm(symbolFor(world, u8"+2/3"), u8"|+2/3|");
	assertForm(symbolFor(world, u8"-2/3"), u8"|-2/3|");
	assertForm(symbolFor(world, u8"+inf.0"), u8"|+inf.0|");
	assertForm(symbolFor(world, u8"-inf.0"), u8"|-inf.0|");
	assertForm(symbolFor(world, u8"+nan.0"), u8"|+nan.0|");
	assertForm(symbolFor(world, u8"-nan.0"), u8"|-nan.0|");
	assertForm(symbolFor(world, u8"+INF.0"), u8"|+INF.0|");
	assertForm(symbolFor(world, u8"-INF.0"), u8"|-INF.0|");
	assertForm(symbolFor(world, u8"+NaN.0"), u8"|+NaN.0|");
	assertForm(symbolFor(world, u8"-NaN.0"), u8"|-NaN.0|");
	assertForm(symbolFor(world, u8"+inf.00"), u8"|+inf.00|");
	assertForm(symbolFor(world, u8"-inf.00"), u8"|-inf.00|");
	assertForm(symbolFor(world, u8"+nan.00"), u8"|+nan.00|");
	assertForm(symbolFor(world, u8"-nan.00"), u8"|-nan.00|");
}

void testString(World &world)
{
	assertForm(stringFor(world, u8"Hello"), u8"\"Hello\"");
	assertForm(stringFor(world, u8"λ"), u8"\"λ\"");
	assertForm(stringFor(world, u8"Hello, world"), u8"\"Hello, world\"");
	assertForm(stringFor(world, u8"Hello\nworld"), u8"\"Hello\\nworld\"");
	assertForm(stringFor(world, u8"Hello\bworld"), u8"\"Hello\\bworld\"");
	assertForm(stringFor(world, u8"Hello\"world"), u8"\"Hello\\\"world\"");
	assertForm(stringFor(world, u8""), u8"\"\"");
	assertForm(stringFor(world, u8"\u0019"), u8"\"\\x19;\"");
}

void testPair(World &world)
{
	SymbolCell *valueA = symbolFor(world, "A");
	SymbolCell *valueB = symbolFor(world, "B");
	SymbolCell *valueC = symbolFor(world, "C");

	assertForm(ProperList<AnyCell>::create(world, {}), "()");
	assertForm(ProperList<AnyCell>::create(world, {valueA}), "(A)");
	assertForm(ProperList<AnyCell>::create(world, {valueA, valueB}), "(A B)");
	assertForm(ProperList<AnyCell>::create(world, {valueA, valueB, valueC}), "(A B C)");

	assertForm(PairCell::createList(world, {valueA}, valueB), "(A . B)");
	assertForm(PairCell::createList(world, {valueA, valueB}, valueC), "(A B . C)");

	// Create a  nested list
	AnyCell *innerList = PairCell::createList(world, {valueA, valueB}, valueC);
	AnyCell *outerList = ProperList<AnyCell>::create(world, {valueA, valueB, valueC, innerList});
	assertForm(outerList, "(A B C (A B . C))");
}

void testBytevector(World &world)
{
	{
		auto *emptyBytevector = BytevectorCell::fromData(world, nullptr, 0);
		assertForm(emptyBytevector, "#u8()");
	}

	{
		uint8_t testData[5] = { 100, 101, 202, 203, 204 };
		auto *testBytevector = BytevectorCell::fromData(world, testData, 5);

		assertForm(testBytevector, "#u8(100 101 202 203 204)");
	}

	{
		CharCell *testChar = CharCell::createInstance(world, UnicodeChar(0x03bb));

		uint8_t testData[5] = { 100, 101, 202, 203, 204 };
		BytevectorCell *testBytevector = BytevectorCell::fromData(world, testData, 5);

		auto *testPair = PairCell::createInstance(world, testChar, testBytevector);
		assertForm(testPair, "(#\\x3bb . #u8(100 101 202 203 204))");
	}
}

void testVector(World &world)
{
	{
		VectorCell *emptyVector = VectorCell::fromFill(world, 0);
		assertForm(emptyVector, "#()");
	}

	{
		VectorCell *fillVector = VectorCell::fromFill(world, 5);

		for(unsigned int i = 0; i < 5; i++)
		{
			auto newInteger = IntegerCell::fromValue(world, i);
			fillVector->setElementAt(i, newInteger);
		}

		assertForm(fillVector, "#(0 1 2 3 4)");
	}
}

void testProcedure(World &world)
{
	// Outputting of pointers isn't consistent across C++ standard libraries
	// This means our null entry point might be output differently on different
	// platforms. The entry point output is just for debugging so there's not
	// point checking it.
	std::string procedureForm = externalFormFor(ProcedureCell::createInstance(world, 0, true, nullptr, nullptr));
	const std::string expectedPrefix("#!procedure(");

	ASSERT_TRUE(procedureForm.compare(0, expectedPrefix.length(), expectedPrefix) == 0);

}

void testCharacter(World &world)
{
	assertForm(CharCell::createInstance(world, UnicodeChar(0x07)), "#\\alarm");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x08)), "#\\backspace");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x7f)), "#\\delete");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x1b)), "#\\escape");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x0a)), "#\\newline");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x00)), "#\\null");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x0d)), "#\\return");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x20)), "#\\space");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x09)), "#\\tab");
	assertForm(CharCell::createInstance(world, UnicodeChar('A')), "#\\A");
	assertForm(CharCell::createInstance(world, UnicodeChar('a')), "#\\a");
	assertForm(CharCell::createInstance(world, UnicodeChar('1')), "#\\1");
	assertForm(CharCell::createInstance(world, UnicodeChar(')')), "#\\)");
	assertForm(CharCell::createInstance(world, UnicodeChar(0x03bb)), "#\\x3bb");
}

void testRecord(World &world)
{
    assertForm(RecordCell::createInstance(world, 0, true, nullptr), "#!record");
}

void testErrorObject(World &world)
{
	StringCell *errorString = StringCell::fromUtf8StdString(world, u8"Test error");

	auto errorObj = ErrorObjectCell::createInstance(world, errorString, EmptyListCell::asProperList<AnyCell>());
	assertForm(errorObj, "#!error(Test error)");

	errorObj = ErrorObjectCell::createInstance(world, errorString, EmptyListCell::asProperList<AnyCell>(), ErrorCategory::Arity);
	assertForm(errorObj, "#!error(arity-error/Test error)");
}

void testPort(World &world)
{
	auto portCell = PortCell::createInstance(world, new StandardOutputPort(std::cout, -1));
	assertForm(portCell, "#!port");
}

void testEofObject()
{
	assertForm(EofObjectCell::instance(), "#!eof");
}

void testMailbox(World &world)
{
	std::shared_ptr<actor::Mailbox> testMailbox(new actor::Mailbox());
	assertForm(MailboxCell::createInstance(world, testMailbox), "#!mailbox");
}

void testHashMap(World &world)
{
	assertForm(HashMapCell::createEmptyInstance(world), "#!hash-map");
}

void testAll(World &world)
{
	testUnit();
	testEmptyList();
	testBoolean();
	testInteger(world);
	testFlonum(world);
	testSymbol(world);
	testString(world);
	testPair(world);
	testBytevector(world);
	testVector(world);
	testProcedure(world);
	testCharacter(world);
	testRecord(world);
	testErrorObject(world);
	testPort(world);
	testEofObject();
	testMailbox(world);
	testHashMap(world);
}

}

int main(int argc, char *argv[])
{
	llcore_run(testAll, argc, argv);
}
