;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This file is generated by gen-types.py. Do not edit manually. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; {unsigned typeId, unsigned gcState}
%datum = type {i16, i16}

; {supertype}
%unspecific = type {%datum}

; {supertype, car, cdr}
%pair = type {%datum, %datum*, %datum*}

; {supertype}
%emptyList = type {%datum}

; {supertype, unsigned charLength, unsigned byteLength, utf8Data}
%stringLike = type {%datum, i32, i32, i8*}

; {supertype}
%string = type {%stringLike}

; {supertype}
%symbol = type {%stringLike}

; {supertype, bool value}
%boolean = type {%datum, i8}

; {supertype}
%numeric = type {%datum}

; {supertype, signed value}
%exactInteger = type {%numeric, i64}

; {supertype, value}
%inexactRational = type {%numeric, double}

; {supertype, unicodeChar}
%character = type {%datum, i32}

; {supertype, unsigned length, data}
%byteVector = type {%datum, i32, i8*}

; {supertype, closure, entryPoint}
%procedure = type {%datum, %closure*, %datum* (%closure*, %datum*)*}

; {supertype, unsigned length, elements}
%vectorLike = type {%datum, i32, %datum**}

; {supertype}
%vector = type {%vectorLike}

; {supertype}
%closure = type {%vectorLike}
