        <li>
          It is a Syntax Error if the LexicallyDeclaredNames of |FunctionStatementList| contains any duplicate entries.
        </li>
        <li>
          It is a Syntax Error if any element of the LexicallyDeclaredNames of |FunctionStatementList| also occurs in the VarDeclaredNames of |FunctionStatementList|.
        </li>
        <li>
          It is a Syntax Error if ContainsDuplicateLabels of |FunctionStatementList| with argument « » is *true*.
        </li>
        <li>
          It is a Syntax Error if ContainsUndefinedBreakTarget of |FunctionStatementList| with argument « » is *true*.
        </li>
        <li>
          It is a Syntax Error if ContainsUndefinedContinueTarget of |FunctionStatementList| with arguments « » and « » is *true*.
        </li>