from typegen.constants import BASE_TYPE

GENERATED_FILE_COMMENT = ("/*****************************************************************\n"
                          " * This file is generated by gen-types.py. Do not edit manually. *\n"
                          " *****************************************************************/\n"
                          "\n")

def type_name_to_clike_class(type_name):
    return "Boxed" + type_name[0].upper() + type_name[1:]

