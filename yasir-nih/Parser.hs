module Parser
  ( parseExpr
  ) where

import Text.Parsec
import Data.Functor.Identity
import Text.Parsec.String (Parser)
import Text.Parsec.Language (javaStyle)
import Text.Parsec.Expr
import qualified Text.Parsec.Token as P

import Expr

parseExpr :: String -> Expr
parseExpr source = case parse pExpr "Expr" source of
  Left e -> error $ "parseExpr: " ++ show e
  Right r -> r

pName :: Parser Name
pName = ident <?> "identifier"

pNameOrSetVar :: Parser Expr
pNameOrSetVar = do
  n <- pName
  pSetVar n <|> pure (Var n)
  where
    pSetVar n = SetVar n <$> (reservedOp ":=" *> pExpr)

pLit :: Parser Expr
pLit = Lit . fromInteger <$> integer <?> "int-literal"

pTerm :: Parser Expr
pTerm = buildExpressionParser table term <?> "compound-term"
  where
    term = parens pExpr <|> pLit <|> pNameOrSetVar <?> "term"
    table = [ [Postfix funcall]
            , [binary "+" Add AssocLeft, binary "-" Sub AssocLeft]
            , [binary "<" LessThan AssocLeft]
            ]
    binary opName op = Infix (reservedOp opName *> pure op)
    funcall = flip Apply <$> parens (commaSep pExpr)

pExpr :: Parser Expr
pExpr = pIf <|> pLet <|> pSeq <|> pLambda <|> pTerm <?> "expr"
  where
    pIf = If
      <$> (reserved "if" *> pExpr)
      <*> (reserved "then" *> pExpr)
      <*> (reserved "else" *> pExpr)

    pLet = Let
      <$> (reserved "let" *> many1 pBinding)
      <*> (reserved "in" *> pExpr)

    pLambda = Lambda
      <$> (reservedOp "\\" *> parens (commaSep pName))
      <*> (reservedOp "->" *> pExpr)

    pSeq = Begin
      <$> braces (many pExpr)

    pBinding = (,) <$> pName <*> (reservedOp "=" *> pExpr)

-- Tokens

lexer :: P.GenTokenParser String u Identity
lexer = P.makeTokenParser (javaStyle { P.reservedNames = words "if then else let in"
                                     , P.reservedOpNames = words "+ - < = \\ -> :="
                                     })

parens :: forall u a. ParsecT String u Identity a -> ParsecT String u Identity a
parens = P.parens lexer

braces :: forall u a. ParsecT String u Identity a -> ParsecT String u Identity a
braces = P.braces lexer

ident :: forall u. ParsecT String u Identity String
ident = P.identifier lexer

reserved :: forall u. String -> ParsecT String u Identity ()
reserved = P.reserved lexer

reservedOp :: forall u. String -> ParsecT String u Identity ()
reservedOp = P.reservedOp lexer

integer :: forall u. ParsecT String u Identity Integer
integer = P.integer lexer

commaSep :: forall u a. ParsecT String u Identity a -> ParsecT String u Identity [a]
commaSep = P.commaSep lexer
