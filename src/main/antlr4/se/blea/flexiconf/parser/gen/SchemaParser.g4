parser grammar SchemaParser;

options { tokenVocab=SchemaLexer; }

// Parser components

document
 : documentationBlock? directiveList
 ;

directiveList
 : directive*
 ;

directive
 : include
 | group
 | use
 | userDirective
 ;

userDirective
 : documentationBlock? directiveName parameterList? flagList? ( LBRACE directiveList RBRACE | SEMI )
 ;

documentationBlock
 : documentationLine+
 ;

documentationLine
 : DOC_START documentationContent DOC_END
 ;

documentationContent
 : DOC_CONTENT?
 ;

include
 : INCLUDE_LITERAL stringArgument SEMI
 ;

group
 : GROUP_LITERAL stringArgument LBRACE directiveList RBRACE
 ;

use
 : USE_LITERAL stringArgument SEMI
 ;

stringArgument
 : quotedStringValue
 | unquotedStringValue
 ;

directiveName
 : UNQUOTED_STRING_LITERAL
 ;

flagList
 : LBRACKET flag ( COMMA flag )* RBRACKET
 ;

flag
 : flagAllowOnce
 | unknown
 ;

flagAllowOnce
 : FLAG_ALLOW_ONCE_LITERAL
 ;

parameterList
 : parameter+
 ;

parameter
 : parameterName COLON parameterValue
 | parameterName
 ;

parameterName
 : UNQUOTED_STRING_LITERAL
 ;

parameterValue
 : integerType
 | decimalType
 | booleanType
 | stringType
 | unknown
 ;

quotedStringValue
 : STRING_LITERAL
 ;

unquotedStringValue
: UNQUOTED_STRING_LITERAL
;

stringType
 : STRING_TYPE_LITERAL
 ;

integerType
 : INT_TYPE_LITERAL
 ;

decimalType
 : DECIMAL_TYPE_LITERAL
 ;

booleanType
 : BOOLEAN_TYPE_LITERAL
 ;

unknown
 : UNQUOTED_STRING_LITERAL
 ;

