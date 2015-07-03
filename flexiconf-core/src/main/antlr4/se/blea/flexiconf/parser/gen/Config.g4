grammar Config;

// Parser components

document
 : directiveList
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
 : directiveName argumentList? ( LBRACE directiveList RBRACE | SEMI )
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

argumentList
 : argument+
 ;

argument
 : quotedStringValue
 | integerValue
 | decimalValue
 | durationValue
 | percentageValue
 | booleanValue
 | unquotedStringValue
 ;

quotedStringValue
 : STRING_LITERAL
 ;

integerValue
 : DECIMAL_INT_LITERAL
 ;

decimalValue
 : DECIMAL_LITERAL
 ;

durationValue
 : DURATION_LITERAL
 ;

percentageValue
 : PERCENTAGE_LITERAL
 ;

booleanValue
 : BOOLEAN_TRUE_LITERAL
 | BOOLEAN_FALSE_LITERAL
 ;

unquotedStringValue
 : UNQUOTED_STRING_LITERAL
 ;

// Lexer components

USE_LITERAL
 : 'use'
 ;

INCLUDE_LITERAL
 : 'include'
 ;

GROUP_LITERAL
 : 'group'
 ;

STRING_LITERAL
 : '"' (~["\\\r\n] | '\\' ~[\r\n])* '"'
 | '\'' (~['\\\r\n] | '\\' ~[\r\n])* '\''
 ;

DECIMAL_INT_LITERAL
 : '0'
 | DECIMAL_SIGN? [1-9] DECIMAL_DIGIT*
 ;

 DECIMAL_SIGN
  : '-'
  ;

DECIMAL_LITERAL
 : DECIMAL_SIGN? DECIMAL_INT_LITERAL '.' DECIMAL_DIGIT+
 | DECIMAL_SIGN? '.' DECIMAL_DIGIT+
 | DECIMAL_INT_LITERAL
 ;

DURATION_LITERAL
 : DECIMAL_LITERAL DURATION_UNIT_LITERAL
 ;

DURATION_UNIT_LITERAL
 : 'ms'
 | 's'
 | 'm'
 | 'h'
 | 'd'
 | 'w'
 | 'M'
 | 'y'
 ;

PERCENTAGE_LITERAL
 : DECIMAL_LITERAL PERCENT
 ;

BOOLEAN_TRUE_LITERAL
 : 'on'
 | 'true'
 | 'y'
 | 'yes'
 ;

BOOLEAN_FALSE_LITERAL
 : 'off'
 | 'false'
 | 'n'
 | 'no'
 ;

fragment DECIMAL_DIGIT
 : [0-9]
 ;

LBRACE
 : '{'
 ;

RBRACE
 : '}'
 ;

SEMI
 : ';'
 ;

PERCENT
 : '%'
 ;

// Ignore comments
COMMENT
 : '#' ( ~[\r\n] )* -> skip
 ;

UNQUOTED_STRING_LITERAL
 : ( ~[;\r\n\t\u000B\u000C\u0020\u00A0] )+
 ;

// Ignore whitespace
WHITESPACE
 : [\r\n\t\u000B\u000C\u0020\u00A0]+ -> skip
 ;
