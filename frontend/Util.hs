module Util where

import Data.Functor.Foldable

cataM :: forall (m :: * -> *) (t :: * -> *) b.
          (Monad m, Traversable t) =>
          (t b -> m b) -> Fix t -> m b
cataM f = (f =<< ) . traverse (cataM f) . unFix
  where
    unFix (Fix f') = f'
