          1. Evaluate |Disjunction| with -1 as its _direction_ argument to obtain a Matcher _m_.
          1. Return a new Matcher with parameters (_x_, _c_) that captures _m_ and performs the following steps when called:
            1. Assert: _x_ is a State.
            1. Assert: _c_ is a Continuation.
            1. Let _d_ be a new Continuation with parameters (_y_) that captures nothing and performs the following steps when called:
              1. Assert: _y_ is a State.
              1. Return _y_.
            1. Call _m_(_x_, _d_) and let _r_ be its result.
            1. If _r_ is ~failure~, return ~failure~.
            1. Let _y_ be _r_'s State.
            1. Let _cap_ be _y_'s _captures_ List.
            1. Let _xe_ be _x_'s _endIndex_.
            1. Let _z_ be the State (_xe_, _cap_).
            1. Call _c_(_z_) and return its result.