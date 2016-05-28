{-# LANGUAGE TemplateHaskell #-}

module Transform where

import Data.Functor.Foldable
import qualified Data.Map as M
import qualified Data.Set as S
import Control.Lens
import Control.Monad.State

import Expr
import Util

-- letToLambda: literally. This is not necessary a optimization, just a simplification.
letToLambda :: Expr -> Expr
letToLambda = cata go
  where
    go = \case
      LetF bs e -> Apply (Lambda (map fst bs) e) (map snd bs)
      x -> Fix x

-- Rename: Give each individual variable a unique name.

data RenameS
  = RenameS
    { _rnEnv :: M.Map Name Name
    , _rnIdGen :: Int
    }

makeLenses ''RenameS

rename :: Expr -> Expr
rename e = ensureNoFree (runState (cataM go e) emptyS)
  where
    go = \case
      VarF v -> Var <$> mkV v
      SetVarF v x -> SetVar <$> mkV v <*> pure x
      LambdaF vs0 x -> do
        -- Rename vs0, and exclude v0s from the env.
        vs <- mapM intro vs0
        pure $ Lambda vs x
      LetF bs0 x -> do
        vs <- mapM (intro . fst) bs0
        pure $ Let (zip vs (map snd bs0)) x
      x -> pure $ Fix x

    emptyS = RenameS M.empty 1

    intro v0 = do
      v <- mkV v0
      rnEnv %= M.delete v0
      pure v

    ensureNoFree s =
      let env = s ^. (_2.rnEnv)
       in if M.null env
          then s ^. _1
          else error $ "Undefined vars: " ++ show env

    mkV :: Name -> State RenameS Name
    mkV v0 = do
      mbV <- uses rnEnv $ M.lookup v0
      case mbV of
        Just v -> pure v
        Nothing -> do
          fresh <- use rnIdGen
          rnIdGen += 1
          let v = v0 ++ "-gen-" ++ show fresh
          rnEnv %= M.insert v0 v
          pure v

-- Box: convert mutable variables into boxed values.

type BoxS = S.Set Name

box :: Expr -> Expr
box e0 = let s = execState (cataM populate e0) emptyS
         in cata (replace s) e0
  where
    emptyS = S.empty

    populate = \case
      SetVarF v _ -> id %= S.insert v
      _ -> pure ()

    replace s = \case
      VarF v -> ifMut ReadBox (Var v) v
      SetVarF v x -> ifMut (const (WriteBox (Var v) x)) (SetVar v x) v
      LetF bs0 x ->
        let bs = map (\(v, e) -> (v, ifMut MkBox e v)) bs0
        in Let bs x
      LambdaF vs0 x ->
        -- Rename mutable args to *-arg and wrap the body with a let that boxes the args.
        let vs = map (\v -> ifMut (++ "-arg") v v) vs0
            mutVs = filter (`S.member` s) vs0
        in Lambda vs (wrapWithBox mutVs x)
      x -> Fix x

      where
        ifMut mkMut x v = if S.member v s
                          then mkMut x
                          else x
        wrapWithBox mutVs = Let (map (\v -> (v, MkBox (Var (v ++ "-arg")))) mutVs)

-- Various transformations.

transformAll :: Expr -> Expr
transformAll = letToLambda . box . rename
