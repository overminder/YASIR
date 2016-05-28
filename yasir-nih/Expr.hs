module Expr where

import Data.Functor.Foldable hiding (Foldable)
import Text.PrettyPrint.ANSI.Leijen hiding ((<$>))

type Name = String

prettyExpr :: Expr -> Doc
prettyExpr = pretty

data ExprF f
  = AddF f f
  | SubF f f
  | LitF Int
  | LessThanF f f
  | VarF Name
  | LetF [(Name, f)] f
  | SetVarF Name f
  | BeginF [f]
  | IfF f f f
  | ApplyF f [f]
  | LambdaF [Name] f
  | MkBoxF f
  | ReadBoxF f
  | WriteBoxF f f
  deriving (Show, Eq, Ord, Functor, Foldable, Traversable)

type Expr = Fix ExprF

pattern Add x y = Fix (AddF x y)
pattern Sub x y = Fix (SubF x y)
pattern LessThan x y = Fix (LessThanF x y)
pattern Var x = Fix (VarF x)
pattern SetVar v x = Fix (SetVarF v x)
pattern Let bs x = Fix (LetF bs x)
pattern Lit x = Fix (LitF x)
pattern If c t f = Fix (IfF c t f)
pattern Begin xs = Fix (BeginF xs)
pattern Apply f xs = Fix (ApplyF f xs)
pattern Lambda vs x = Fix (LambdaF vs x)
pattern MkBox x = Fix (MkBoxF x)
pattern ReadBox b = Fix (ReadBoxF b)
pattern WriteBox b x = Fix (WriteBoxF b x)

instance Pretty Expr where
  pretty = cata go
    where
      go = \case
        AddF a b -> parens (a <+> text "+" <+> b)
        SubF a b -> parens (a <+> text "-" <+> b)
        LessThanF a b -> parens (a <+> text "<" <+> b)
        VarF x -> pretty x
        LetF bs x -> text "let" <$$> indent 2 (vsep . map pBinding $ bs) <$$> text "in" <+> x
        SetVarF n x -> pretty n <+> text ":=" <+> x
        LitF i -> pretty i
        BeginF xs -> text "{" <$$> indent 2 (vsep xs) <$$> text "}"
        IfF c t f -> parens (text "if" <+> c
          <+> text "then" <+> t
          <+> text "else" <+> f)
        ApplyF f xs -> f <> args xs
        LambdaF vs x -> parens (text "\\" <> args vs <+> text "->" <+> x)
        MkBoxF x -> text "box" <+> x
        ReadBoxF b -> text "read-box" <+> b
        WriteBoxF b x -> text "write-box" <+> b <+> x

      args :: Pretty a => [a] -> Doc
      args = parens . cat . punctuate comma . map pretty

      pBinding (name, x) = pretty name <+> equals <+> x
