lexer grammar SchemaLexer;

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

STRING_TYPE_LITERAL
 : 'String'
 ;

INT_TYPE_LITERAL
 : 'Int'
 ;

DECIMAL_TYPE_LITERAL
 : 'Decimal'
 ;

DURATION_TYPE_LITERAL
 : 'Duration'
 ;

PERCENTAGE_TYPE_LITERAL
 : 'Percentage'
 ;

BOOLEAN_TYPE_LITERAL
 : 'Bool'
 ;

FLAG_ALLOW_ONCE_LITERAL
 : 'once'
 ;

EQ
 : '='
 ;

COLON
 : ':'
 ;

COMMA
 : ','
 ;

LBRACE
 : '{'
 ;

RBRACE
 : '}'
 ;

LBRACKET
 : '['
 ;

RBRACKET
 : ']'
 ;

SEMI
 : ';'
 ;

DOC_START
 : '##' [ \t]* -> pushMode(DOC)
 ;

// Ignore comments
COMMENT
 : '#' ~'#' ( ~[\r\n] )* -> skip
 ;

STRING_LITERAL
 : '"' ( ~["\r\n] )* '"'
 | '\'' (  ~['\r\n] )* '\''
 ;

UNQUOTED_STRING_LITERAL
 : ( ~[#=,\[\]:;\r\n\t\u000B\u000C\u0020\u00A0] )+
 ;

// Ignore whitespace
WHITESPACE
 : [\r\n\t\u000B\u000C\u0020\u00A0]+ -> skip
 ;

mode DOC;

DOC_CONTENT
 : ( ~[\r\n] )+
 ;

DOC_END
 : '\r'? '\n' -> popMode
 ;
