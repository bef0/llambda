/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

#include "binding/UnitCell.h"
#include "binding/PairCell.h"
#include "binding/EmptyListCell.h"
#include "binding/StringCell.h"
#include "binding/SymbolCell.h"
#include "binding/BooleanCell.h"
#include "binding/ExactIntegerCell.h"
#include "binding/FlonumCell.h"
#include "binding/CharCell.h"
#include "binding/VectorCell.h"
#include "binding/BytevectorCell.h"
#include "binding/ProcedureCell.h"
#include "binding/RecordCell.h"
#include "binding/ErrorObjectCell.h"
#include "binding/PortCell.h"
#include "binding/EofObjectCell.h"
#include "binding/DynamicStateCell.h"
#include "binding/MailboxCell.h"

using lliby::alloc::AllocCell;

static_assert(sizeof(lliby::UnitCell) <= sizeof(AllocCell), "UnitCell does not fit in to a cell");
static_assert(sizeof(lliby::PairCell) <= sizeof(AllocCell), "PairCell does not fit in to a cell");
static_assert(sizeof(lliby::EmptyListCell) <= sizeof(AllocCell), "EmptyListCell does not fit in to a cell");
static_assert(sizeof(lliby::StringCell) <= sizeof(AllocCell), "StringCell does not fit in to a cell");
static_assert(sizeof(lliby::InlineStringCell) <= sizeof(AllocCell), "InlineStringCell does not fit in to a cell");
static_assert(sizeof(lliby::HeapStringCell) <= sizeof(AllocCell), "HeapStringCell does not fit in to a cell");
static_assert(sizeof(lliby::SymbolCell) <= sizeof(AllocCell), "SymbolCell does not fit in to a cell");
static_assert(sizeof(lliby::InlineSymbolCell) <= sizeof(AllocCell), "InlineSymbolCell does not fit in to a cell");
static_assert(sizeof(lliby::HeapSymbolCell) <= sizeof(AllocCell), "HeapSymbolCell does not fit in to a cell");
static_assert(sizeof(lliby::BooleanCell) <= sizeof(AllocCell), "BooleanCell does not fit in to a cell");
static_assert(sizeof(lliby::ExactIntegerCell) <= sizeof(AllocCell), "ExactIntegerCell does not fit in to a cell");
static_assert(sizeof(lliby::FlonumCell) <= sizeof(AllocCell), "FlonumCell does not fit in to a cell");
static_assert(sizeof(lliby::CharCell) <= sizeof(AllocCell), "CharCell does not fit in to a cell");
static_assert(sizeof(lliby::VectorCell) <= sizeof(AllocCell), "VectorCell does not fit in to a cell");
static_assert(sizeof(lliby::BytevectorCell) <= sizeof(AllocCell), "BytevectorCell does not fit in to a cell");
static_assert(sizeof(lliby::ProcedureCell) <= sizeof(AllocCell), "ProcedureCell does not fit in to a cell");
static_assert(sizeof(lliby::RecordCell) <= sizeof(AllocCell), "RecordCell does not fit in to a cell");
static_assert(sizeof(lliby::ErrorObjectCell) <= sizeof(AllocCell), "ErrorObjectCell does not fit in to a cell");
static_assert(sizeof(lliby::PortCell) <= sizeof(AllocCell), "PortCell does not fit in to a cell");
static_assert(sizeof(lliby::EofObjectCell) <= sizeof(AllocCell), "EofObjectCell does not fit in to a cell");
static_assert(sizeof(lliby::DynamicStateCell) <= sizeof(AllocCell), "DynamicStateCell does not fit in to a cell");
static_assert(sizeof(lliby::MailboxCell) <= sizeof(AllocCell), "MailboxCell does not fit in to a cell");
