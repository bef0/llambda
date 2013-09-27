/*****************************************************************
 * This file is generated by gen-types.py. Do not edit manually. *
 *****************************************************************/

#include "binding/BoxedDatum.h"

using namespace lliby;

extern "C"
{

bool lliby_is_unspecific(const BoxedDatum *value)
{
	return value->isBoxedUnspecific();
}

bool lliby_is_pair(const BoxedDatum *value)
{
	return value->isBoxedPair();
}

bool lliby_is_empty_list(const BoxedDatum *value)
{
	return value->isBoxedEmptyList();
}

bool lliby_is_string_like(const BoxedDatum *value)
{
	return value->isBoxedStringLike();
}

bool lliby_is_string(const BoxedDatum *value)
{
	return value->isBoxedString();
}

bool lliby_is_symbol(const BoxedDatum *value)
{
	return value->isBoxedSymbol();
}

bool lliby_is_boolean(const BoxedDatum *value)
{
	return value->isBoxedBoolean();
}

bool lliby_is_numeric(const BoxedDatum *value)
{
	return value->isBoxedNumeric();
}

bool lliby_is_exact_integer(const BoxedDatum *value)
{
	return value->isBoxedExactInteger();
}

bool lliby_is_inexact_rational(const BoxedDatum *value)
{
	return value->isBoxedInexactRational();
}

bool lliby_is_character(const BoxedDatum *value)
{
	return value->isBoxedCharacter();
}

bool lliby_is_byte_vector(const BoxedDatum *value)
{
	return value->isBoxedByteVector();
}

bool lliby_is_procedure(const BoxedDatum *value)
{
	return value->isBoxedProcedure();
}

bool lliby_is_vector_like(const BoxedDatum *value)
{
	return value->isBoxedVectorLike();
}

bool lliby_is_vector(const BoxedDatum *value)
{
	return value->isBoxedVector();
}

bool lliby_is_closure(const BoxedDatum *value)
{
	return value->isBoxedClosure();
}


}
