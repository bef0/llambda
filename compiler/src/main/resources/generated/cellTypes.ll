;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This file is generated by typegen. Do not edit manually. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; {supertype, typeId, gcState}
%datum = type {i8, i8}
!10 = metadata !{ metadata !"Datum::typeId" }
!11 = metadata !{ metadata !"Datum::gcState" }

; {supertype}
%unit = type {%datum}
!12 = metadata !{ metadata !"Datum::typeId->Unit", metadata !10 }
!13 = metadata !{ metadata !"Datum::gcState->Unit", metadata !11 }

; {supertype}
%listElement = type {%datum}
!14 = metadata !{ metadata !"Datum::typeId->ListElement", metadata !10 }
!15 = metadata !{ metadata !"Datum::gcState->ListElement", metadata !11 }

; {supertype, memberTypeId, unsigned listLength, car, cdr}
%pair = type {%listElement, i8, i32, %datum*, %datum*}
!16 = metadata !{ metadata !"Datum::typeId->ListElement->Pair", metadata !14 }
!17 = metadata !{ metadata !"Datum::gcState->ListElement->Pair", metadata !15 }
!18 = metadata !{ metadata !"Pair::memberTypeId" }
!19 = metadata !{ metadata !"Pair::listLength" }
!20 = metadata !{ metadata !"Pair::car" }
!21 = metadata !{ metadata !"Pair::cdr" }

; {supertype}
%emptyList = type {%listElement}
!22 = metadata !{ metadata !"Datum::typeId->ListElement->EmptyList", metadata !14 }
!23 = metadata !{ metadata !"Datum::gcState->ListElement->EmptyList", metadata !15 }

; {supertype, unsigned allocSlackBytes, unsigned charLength, unsigned byteLength}
%string = type {%datum, i16, i32, i32}
!24 = metadata !{ metadata !"Datum::typeId->String", metadata !10 }
!25 = metadata !{ metadata !"Datum::gcState->String", metadata !11 }
!26 = metadata !{ metadata !"String::allocSlackBytes" }
!27 = metadata !{ metadata !"String::charLength" }
!28 = metadata !{ metadata !"String::byteLength" }

; {supertype, inlineData}
%inlineString = type {%string, [12 x i8]}
!29 = metadata !{ metadata !"InlineString::inlineData" }

; {supertype, heapByteArray}
%heapString = type {%string, %sharedByteArray*}
!30 = metadata !{ metadata !"HeapString::heapByteArray" }

; {supertype, unsigned charLength, unsigned byteLength}
%symbol = type {%datum, i32, i32}
!31 = metadata !{ metadata !"Datum::typeId->Symbol", metadata !10 }
!32 = metadata !{ metadata !"Datum::gcState->Symbol", metadata !11 }
!33 = metadata !{ metadata !"Symbol::charLength" }
!34 = metadata !{ metadata !"Symbol::byteLength" }

; {supertype, inlineData}
%inlineSymbol = type {%symbol, [12 x i8]}
!35 = metadata !{ metadata !"InlineSymbol::inlineData" }

; {supertype, heapByteArray}
%heapSymbol = type {%symbol, %sharedByteArray*}
!36 = metadata !{ metadata !"HeapSymbol::heapByteArray" }

; {supertype, bool value}
%boolean = type {%datum, i8}
!37 = metadata !{ metadata !"Datum::typeId->Boolean", metadata !10 }
!38 = metadata !{ metadata !"Datum::gcState->Boolean", metadata !11 }
!39 = metadata !{ metadata !"Boolean::value" }

; {supertype}
%numeric = type {%datum}
!40 = metadata !{ metadata !"Datum::typeId->Numeric", metadata !10 }
!41 = metadata !{ metadata !"Datum::gcState->Numeric", metadata !11 }

; {supertype, signed value}
%exactInteger = type {%numeric, i64}
!42 = metadata !{ metadata !"Datum::typeId->Numeric->ExactInteger", metadata !40 }
!43 = metadata !{ metadata !"Datum::gcState->Numeric->ExactInteger", metadata !41 }
!44 = metadata !{ metadata !"ExactInteger::value" }

; {supertype, value}
%inexactRational = type {%numeric, double}
!45 = metadata !{ metadata !"Datum::typeId->Numeric->InexactRational", metadata !40 }
!46 = metadata !{ metadata !"Datum::gcState->Numeric->InexactRational", metadata !41 }
!47 = metadata !{ metadata !"InexactRational::value" }

; {supertype, unicodeChar}
%character = type {%datum, i32}
!48 = metadata !{ metadata !"Datum::typeId->Character", metadata !10 }
!49 = metadata !{ metadata !"Datum::gcState->Character", metadata !11 }
!50 = metadata !{ metadata !"Character::unicodeChar" }

; {supertype, unsigned length, elements}
%vector = type {%datum, i32, %datum**}
!51 = metadata !{ metadata !"Datum::typeId->Vector", metadata !10 }
!52 = metadata !{ metadata !"Datum::gcState->Vector", metadata !11 }
!53 = metadata !{ metadata !"Vector::length" }
!54 = metadata !{ metadata !"Vector::elements" }

; {supertype, unsigned length, byteArray}
%bytevector = type {%datum, i32, %sharedByteArray*}
!55 = metadata !{ metadata !"Datum::typeId->Bytevector", metadata !10 }
!56 = metadata !{ metadata !"Datum::gcState->Bytevector", metadata !11 }
!57 = metadata !{ metadata !"Bytevector::length" }
!58 = metadata !{ metadata !"Bytevector::byteArray" }

; {supertype, bool dataIsInline, unsigned recordClassId, recordData}
%recordLike = type {%datum, i8, i32, i8*}
!59 = metadata !{ metadata !"Datum::typeId->RecordLike", metadata !10 }
!60 = metadata !{ metadata !"Datum::gcState->RecordLike", metadata !11 }
!61 = metadata !{ metadata !"RecordLike::dataIsInline" }
!62 = metadata !{ metadata !"RecordLike::recordClassId" }
!63 = metadata !{ metadata !"RecordLike::recordData" }

; {supertype, entryPoint}
%procedure = type {%recordLike, %datum* (%world*, %procedure*, %listElement*)*}
!64 = metadata !{ metadata !"Datum::typeId->RecordLike->Procedure", metadata !59 }
!65 = metadata !{ metadata !"Datum::gcState->RecordLike->Procedure", metadata !60 }
!66 = metadata !{ metadata !"RecordLike::dataIsInline->Procedure", metadata !61 }
!67 = metadata !{ metadata !"RecordLike::recordClassId->Procedure", metadata !62 }
!68 = metadata !{ metadata !"RecordLike::recordData->Procedure", metadata !63 }
!69 = metadata !{ metadata !"Procedure::entryPoint" }

; {supertype, extraData}
%record = type {%recordLike, i8*}
!70 = metadata !{ metadata !"Datum::typeId->RecordLike->Record", metadata !59 }
!71 = metadata !{ metadata !"Datum::gcState->RecordLike->Record", metadata !60 }
!72 = metadata !{ metadata !"RecordLike::dataIsInline->Record", metadata !61 }
!73 = metadata !{ metadata !"RecordLike::recordClassId->Record", metadata !62 }
!74 = metadata !{ metadata !"RecordLike::recordData->Record", metadata !63 }
!75 = metadata !{ metadata !"Record::extraData" }

; {supertype, message, irritants}
%errorObject = type {%datum, %string*, %listElement*}
!76 = metadata !{ metadata !"Datum::typeId->ErrorObject", metadata !10 }
!77 = metadata !{ metadata !"Datum::gcState->ErrorObject", metadata !11 }
!78 = metadata !{ metadata !"ErrorObject::message" }
!79 = metadata !{ metadata !"ErrorObject::irritants" }
