          1. If _value_ is not present, let _s_ be the empty String.
          1. Else,
            1. If NewTarget is *undefined* and Type(_value_) is Symbol, return SymbolDescriptiveString(_value_).
            1. Let _s_ be ? ToString(_value_).
          1. If NewTarget is *undefined*, return _s_.
          1. Return ! StringCreate(_s_, ? GetPrototypeFromConstructor(NewTarget, *"%String.prototype%"*)).