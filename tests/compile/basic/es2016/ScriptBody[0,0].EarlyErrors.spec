        <li>
          It is a Syntax Error if |StatementList| Contains `super` unless the source code containing `super` is eval code that is being processed by a direct eval that is contained in function code that is not the function code of an |ArrowFunction|.
        </li>
        <li>
          It is a Syntax Error if |StatementList| Contains |NewTarget| unless the source code containing |NewTarget| is eval code that is being processed by a direct eval that is contained in function code that is not the function code of an |ArrowFunction|.
        </li>
        <li>
          It is a Syntax Error if ContainsDuplicateLabels of |StatementList| with argument « » is *true*.
        </li>
        <li>
          It is a Syntax Error if ContainsUndefinedBreakTarget of |StatementList| with argument « » is *true*.
        </li>
        <li>
          It is a Syntax Error if ContainsUndefinedContinueTarget of |StatementList| with arguments « » and « » is *true*.
        </li>