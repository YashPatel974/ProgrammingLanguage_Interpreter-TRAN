import AST.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private TranNode tran;
    //private List<Token> input_tokens;
    private TokenManager tokens_list;

    public Parser(TranNode top, List<Token> tokens) {
        this.tokens_list = new TokenManager(tokens);

        this.tran = top;
        //this.input_tokens = tokens;
    }

    //Tran = {Interface } | [Class]
    public void Tran() throws SyntaxErrorException {
        while (!tokens_list.done()){
            // if Interface token found, keep running while loop and keep adding all the interfaces.
            while(tokens_list.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()){
                tran.Interfaces.add(parseInterface());
            }
            // after parsing all the Interfaces, checking if we're at the end of the file.
            if(tokens_list.done()){
                break;
            }else{
                // After checking for Interfaces in above while loop, check weather class is present.
                if (!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent() && tokens_list.matchAndRemove(Token.TokenTypes.CLASS).isPresent()){
                    tran.Classes.add(parseClass());
               }
            }

        }

    }

    //parse Class.

    //Class = "class" Identifier [ "implements" Identifier { "," Identifier } ] NEWLINE INDENT {
        //Constructor NEWLINE | MethodDeclaration NEWLINE | Member NEWLINE } DEDENT
    public ClassNode parseClass() throws SyntaxErrorException {
        ClassNode local_classNode = new ClassNode();

//        while (!tokens_list.done()){
            Optional<Token> identifier = tokens_list.matchAndRemove(Token.TokenTypes.WORD);
            if(identifier.isPresent()){
                // Name of the CLASS.
                local_classNode.name = identifier.get().getValue();

                // if our class implements any interfaces
                if(tokens_list.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()){
                    // name of node being implemented.
                    if (tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.WORD) {

                        do {
                            //List of the name of Interfaces getting implemented.
                            local_classNode.interfaces.add(tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue());
                            // if the next two tokens doesn't follow the {"," Identifier} pattern.
                            if (!tokens_list.nextTwoTokensMatch(Token.TokenTypes.COMMA,Token.TokenTypes.WORD)){
                                break;
                            }

                        }while (tokens_list.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
                    }
                }

                //after getting all the implemented interfaces, NEXT STEP.
                //tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()
                if (RequireNewLine()){
                    if (tokens_list.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){

                        // Getting all the Constructors,MethodDeclarations,Members till I hit a DEDENT.
                        //{Constructor NEWLINE | MethodDeclaration NEWLINE | Member NEWLINE }
                        while(!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent() && !tokens_list.done()){

                            if(tokens_list.matchAndRemove(Token.TokenTypes.CONSTRUCT).isPresent()){
                                //Call Constructor Function to handle with constructor.
                                //adding all constructors.
                                local_classNode.constructors.add(parseConstructor());
                                RequireNewLine();
                            } else{
                                //Handling methods inside class.
                                Optional<Token> method_type = (tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.SHARED)? tokens_list.matchAndRemove(Token.TokenTypes.SHARED): tokens_list.matchAndRemove(Token.TokenTypes.PRIVATE);

                                if(method_type.isPresent() || tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.LPAREN)){
                                    //Call Method Declarations.
                                    local_classNode.methods.add(parseMethodDeclaration(method_type));
                                    RequireNewLine();
                                }
                            }
                            while (!tokens_list.done()  && isValidVariableDeclaration()){
                                local_classNode.members.add(parseMember());
                            }
                        }


                    }else {
                        throw new SyntaxErrorException("Should Have Indent (Class) !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                    }
                }else {
                    throw new SyntaxErrorException("NewLine Not Present !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                }

            }else {
                throw new SyntaxErrorException("Should have word after Classname !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
            }
        //}


        return local_classNode;
    }

    //parse Member
    //VariableDeclaration ["accessor:" Statements] ["mutator:" Statements]
    public MemberNode parseMember() throws SyntaxErrorException {
        MemberNode local_memberNode = new MemberNode();
        local_memberNode.declaration = parseVariableDeclarations();

        if(RequireNewLine()){
            // checking for Indent because we need Indent to have ACCESSOR or MUTATOR for any member.
            //But not throwing error cause If we don't have Indent then it's fine, we might have something else after member.
            if (tokens_list.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
                if (tokens_list.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()){
                    if (tokens_list.matchAndRemove(Token.TokenTypes.COLON).isPresent()){

                        //local_memberNode.accessor = Optional.of(parseStatementS());
                    }else {
                        throw new SyntaxErrorException("No Colon Present!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                    }
                } else if (tokens_list.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                    if (tokens_list.matchAndRemove(Token.TokenTypes.COLON).isPresent()){
                        //local_memberNode.mutator = Optional.of(parseStatementS());
                    }else {
                        throw new SyntaxErrorException("No Colon Present!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                    }
                }
            }

        }else {
            if(!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                    throw new SyntaxErrorException("New Line Needed after Member!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
            }
        }


//        if ( !tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent() && !tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
//            throw new SyntaxErrorException("No NewLine Present after class member!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
//        }


        return local_memberNode;
    }

    //Parse Constructor
    //Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    public ConstructorNode parseConstructor() throws SyntaxErrorException {
        ConstructorNode local_constructorNode = new ConstructorNode();
        if (tokens_list.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()){
            //consider Do-while loop just in case.
            while (!tokens_list.nextTwoTokensMatch(Token.TokenTypes.RPAREN,Token.TokenTypes.NEWLINE)){

                if(tokens_list.nextTwoTokensMatch(Token.TokenTypes.COMMA,Token.TokenTypes.WORD)){
                    tokens_list.matchAndRemove(Token.TokenTypes.COMMA);
                }
                // Adding constructor Parameters.
                local_constructorNode.parameters.add(parseVariableDeclarations());


            }
        }else {
            throw new SyntaxErrorException("Left Parenthesis Not Present (Constructor) !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }

        if (tokens_list.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()){
            if(RequireNewLine()){
                // Method Body.
                if (tokens_list.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){

                    // Variable declarations
                    while(isValidVariableDeclaration()){
                        local_constructorNode.locals.add(parseVariableDeclarations());

                        // Checking if there's no new line after declaring one variable
                        //eating up the newline token if it is present.
                        if(!tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                            throw new SyntaxErrorException("Need NewLine after Variable Declaration (Constructor) !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                        }
                    }

                    //Handling Statements.
                    //  {Statement} DEDENT
                    while (!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent() && !tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                        if (tokens_list.matchAndRemove(Token.TokenTypes.IF).isPresent()){
                            // parsing if conditional statements

                            StatementNode temp = parseIFStatement();
                            if (temp != null){
                                local_constructorNode.statements.add(temp);
                            }
                            //|| tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.EQUAL)
                        } else if (tokens_list.matchAndRemove(Token.TokenTypes.LOOP).isPresent()) {
                            // parsing LOOP.

                            local_constructorNode.statements.add(parseLoop());
                        } else {

                            local_constructorNode.statements.add(disambiguate().get());
                        }



                        // Eating up all the potential new lines
                        RequireNewLine();
                    }


                }else {
                    throw new SyntaxErrorException("No Indentation present !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                }
            }else {
                throw new SyntaxErrorException("New Line Not Present !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
            }
        }else {
            throw new SyntaxErrorException("There should be Right Parenthesis !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }
        return local_constructorNode;
    }

    //If = "if" BoolExp NEWLINE Statements ["else" NEWLINE (Statement | Statements)]

    public IfNode parseIFStatement() throws SyntaxErrorException {
        IfNode local_ifNode = new IfNode();
        local_ifNode.elseStatement = Optional.empty();
        local_ifNode.condition = parseBooleanExpTerm();


        if (RequireNewLine()){
            // adding ALL the statements I found in the List.
            List<StatementNode> temp = parseStatementS();
            if (temp!= null){
                local_ifNode.statements = temp;
            }


            // there is else condition.
            if (tokens_list.matchAndRemove(Token.TokenTypes.ELSE).isPresent()){
                if (RequireNewLine()){
                    //adding ElseNode
                    local_ifNode.elseStatement = Optional.ofNullable(parseElseStatement());
                }else {
                    throw new SyntaxErrorException("NewLine Needed !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                }
            }
        }else {
            throw new SyntaxErrorException("NewLine Not Present (IF-Condition)!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }
        return local_ifNode;
    }

    /*============================
    *   Else Statement
    * ============================*/

    public ElseNode parseElseStatement() throws SyntaxErrorException {
        ElseNode local_elseNode = new ElseNode();
        //local_elseNode.statements.addAll(parseStatementS());
        local_elseNode.statements = parseStatementS();
        return local_elseNode;
    }



    /*====================================================
    *  Parsing LOOP Here
    ======================================================*/
    public LoopNode parseLoop() throws SyntaxErrorException {
        LoopNode local_loopNode = new LoopNode();
        // Optional<Token> temp = tokens_list.matchAndRemove(Token.TokenTypes.WORD);
        //if (temp.isPresent()){
        // local_loopNode.assignment = parseVariableDeclaration(temp.get());
        
        
        
        // loop n = x.times();
        if (tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)){

            Optional<Token> temp = tokens_list.matchAndRemove(Token.TokenTypes.WORD);
            if (temp.isPresent()){
                local_loopNode.assignment = parseVariableRefrance(temp.get());
            }else {
                local_loopNode.assignment = Optional.empty();
            }
                tokens_list.matchAndRemove(Token.TokenTypes.ASSIGN);
        }else {
            local_loopNode.assignment = Optional.empty();
        }

        local_loopNode.expression = parseBooleanExpTerm();
        if (RequireNewLine()){
            local_loopNode.statements.addAll(parseStatementS());
        }
        return local_loopNode;
    }
    public Optional<VariableReferenceNode> parseVariableRefrance(Token temper) throws SyntaxErrorException {
        VariableReferenceNode local_variableReferenceNode = new VariableReferenceNode();
        local_variableReferenceNode.name = temper.getValue();
        return Optional.of(local_variableReferenceNode);
    }


    /*
        disambiguate
        MethodCall | Assignment

        MethodCall = [VariableReference { "," VariableReference } "="] MethodCallExpression
        Assignment = VariableReference "=" Expression

    */

    Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        MethodCallStatementNode local_methodCallNode = new MethodCallStatementNode();
        AssignmentNode local_assignmentNode = new AssignmentNode();

        ExpressionNode local_variableReferenceNode;


        //List<VariableReferenceNode> returnVariableDeclarations = new ArrayList<>();
        //local_variableReferenceNode =  parseVariableReference();
        //if (local_variableReferenceNode != null){
            if (tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.COMMA)) {
                //local_variableReferenceNode =  parseVariableReference();
                do {
                    local_methodCallNode.returnValues.add((VariableReferenceNode) parseVariableReference());
                    if (tokens_list.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()){
                        break;
                    }

                }while (tokens_list.matchAndRemove(Token.TokenTypes.COMMA).isPresent());

                MethodCallExpressionNode local_expression = (MethodCallExpressionNode) parseMethodCallExpression().get();
                local_methodCallNode.objectName = local_expression.objectName;
                local_methodCallNode.methodName = local_expression.methodName;
                local_methodCallNode.parameters = local_expression.parameters;

                return Optional.of(local_methodCallNode);
            } else if (tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.DOT)) {
                MethodCallExpressionNode local_expression = (MethodCallExpressionNode) parseMethodCallExpression().get();
                local_methodCallNode.objectName = local_expression.objectName;
                local_methodCallNode.methodName = local_expression.methodName;
                local_methodCallNode.parameters = local_expression.parameters;
                return Optional.of(local_methodCallNode);

            } else if (tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.ASSIGN)) {
            //TYPE CASTING IT TO VariableReferenceNode
                local_variableReferenceNode =  parseVariableReference();
                tokens_list.matchAndRemove(Token.TokenTypes.ASSIGN);


                local_assignmentNode.target = (VariableReferenceNode) local_variableReferenceNode;
                local_assignmentNode.expression = parseExpresion();
            }


        return Optional.of(local_assignmentNode);
    }


    /*============================
    *       STATEMENT(S)
    * ============================*/
    //INDENT {Statement NEWLINE } DEDENT
    public List<StatementNode> parseStatementS() throws SyntaxErrorException {
        List<StatementNode> local_statementNodes = new LinkedList<>();
        // If I'm not at the end of the file, and  I'm having an Indent
        if (tokens_list.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
            // running loop till I hit a DEDENT token.

            while (!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent() && !tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                if(tokens_list.matchAndRemove(Token.TokenTypes.IF).isPresent()){
                    //parsing IF
                    local_statementNodes.add(parseIFStatement());
                }else if(tokens_list.matchAndRemove(Token.TokenTypes.LOOP).isPresent() || tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.EQUAL)){
                    //Parsing loop
                    local_statementNodes.add(parseLoop());
                }else {

                    local_statementNodes.add(disambiguate().get());
                    if (!tokens_list.nextTwoTokensMatch(Token.TokenTypes.NEWLINE , Token.TokenTypes.DEDENT)){
                        tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE);
                    }

                }
            }
        }else {
            if ((tokens_list.peek().isPresent() && tokens_list.peek().get().getType() != Token.TokenTypes.DEDENT)){
                throw new SyntaxErrorException("No Indent Present !(Statement)", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
            }

        }

        tokens_list.matchAndRemove(Token.TokenTypes.DEDENT);

        return local_statementNodes;
    }


    /*==============================================================================
    *  Parsing METHOD DECLARATIONS.
    * ==============================================================================*/
    // ["private"] ["shared"] MethodHeader NEWLINE MethodBody
    public MethodDeclarationNode parseMethodDeclaration(Optional<Token> method_type) throws SyntaxErrorException {
        MethodDeclarationNode local_methodDeclarationNode = new MethodDeclarationNode();
        // calling the parseMethodHeader to handle the header of our Method Declaration.
        //eating up the NewLine after Parenthesis in parseMethodHeader
        MethodHeaderNode header = parseMethodHeader();


        // PRIVATE OR SHARED METHOD.
        if (method_type.equals(Token.TokenTypes.SHARED)){
            local_methodDeclarationNode.isShared = true;
        }else if (method_type.equals(Token.TokenTypes.PRIVATE)){
            local_methodDeclarationNode.isPrivate = true;
        }else{
            local_methodDeclarationNode.isShared = true;
        }


            // Parse Method Header handling the Method Name, Parameters and Returns.
            local_methodDeclarationNode.name = header.name;
            local_methodDeclarationNode.parameters = header.parameters;
            local_methodDeclarationNode.returns = header.returns;


            //if (tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                //Method Body
                //INDENT { VariableDeclaration NEWLINE } {Statement} DEDENT
                //============================================================================================================
                    if (tokens_list.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){

                        // Variable declarations
                        while(isValidVariableDeclaration()){
                            //local_constructorNode.locals.add(parseVariableDeclarations());

                            local_methodDeclarationNode.locals.add(parseVariableDeclarations());

                            // Checking if there's no new line after declaring one variable
                            //eating up the newline token if it is present.
                            if(!tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                                throw new SyntaxErrorException("Need NewLine after Variable Declaration (Method_Declaration) !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                            }
                        }


                        //Handling Statements.
                        //  {Statement} DEDENT // && !tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()
                        while (!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                            tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE);
                            if (tokens_list.matchAndRemove(Token.TokenTypes.IF).isPresent()){
                                // parsing if conditional statements

                                StatementNode temp = parseIFStatement();
                                if (temp != null){
                                    local_methodDeclarationNode.statements.add(temp);
                                }

                            } else if (tokens_list.matchAndRemove(Token.TokenTypes.LOOP).isPresent() || tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.EQUAL)) {
                                // parsing LOOP.

                                local_methodDeclarationNode.statements.add(parseLoop());
                            } else {

                                local_methodDeclarationNode.statements.add(disambiguate().get());
                            }



                            // Eating up all the potential new lines
                            RequireNewLine();
                        }


                    }else {
                        throw new SyntaxErrorException("Indent Needed !(Class -> Method_Declaration.)", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                    }

                //============================================================================================================

//            }else {
//                throw new SyntaxErrorException("NewLine is not present! ", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
//            }


        return local_methodDeclarationNode;
    }



    /*
        Date: 10/21/2024
        parser-3
    * */
     /*================================================================================================================
                                 BookExpTerm , BoolExpFactor, Expression, MethodCallExpression(returning empty)
    ================================================================================================================*/
    //BoolExpTerm = BoolExpFactor {("and"|"or") BoolExpTerm} | "not" BoolExpTerm
     public ExpressionNode parseBooleanExpTerm() throws SyntaxErrorException {
         BooleanOpNode local_booleanOpNode = new BooleanOpNode();
         BooleanOpNode local_BooleanOpNodeTWO = new BooleanOpNode();
         NotOpNode local_notOpNode = new NotOpNode();

        // Eating up "not" token here.
         if(tokens_list.matchAndRemove(Token.TokenTypes.NOT).isPresent()){
             //recursive calling this parseBooleanExpTerm() function.
             local_notOpNode.left = parseBooleanExpTerm();
         } else{
             local_booleanOpNode.left = BoolExpFactor();

             //Eating up the And , OR tokens here if we found one.
             //local_booleanOpNode.op = null;
             if(tokens_list.matchAndRemove(Token.TokenTypes.AND).isPresent()){

                 local_BooleanOpNodeTWO.left = local_booleanOpNode.left;
                 local_BooleanOpNodeTWO.op = BooleanOpNode.BooleanOperations.and;

                 local_BooleanOpNodeTWO.right = BoolExpFactor();

                 local_booleanOpNode.left = local_BooleanOpNodeTWO;

                 if (tokens_list.peek().isPresent() &&
                         (tokens_list.peek().get().getType() == Token.TokenTypes.AND || tokens_list.peek().get().getType() == Token.TokenTypes.OR)){


                     if(tokens_list.matchAndRemove(Token.TokenTypes.AND).isPresent()){

                         local_booleanOpNode.right = parseBooleanExpTerm();
                     } else if (tokens_list.matchAndRemove(Token.TokenTypes.OR).isPresent()) {
                         local_booleanOpNode.right = parseBooleanExpTerm();
                     }

                 }

             } else if (tokens_list.matchAndRemove(Token.TokenTypes.OR).isPresent()) {
                 local_booleanOpNode.op = BooleanOpNode.BooleanOperations.or;
                 local_booleanOpNode.right = parseBooleanExpTerm();
             } else{
                 return local_booleanOpNode.left;
             }
         }

         return local_booleanOpNode;
     }

/*
Variable Reference
tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.WORD
*/
//BoolExpFactor = (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression) | Expression

    public ExpressionNode BoolExpFactor() throws SyntaxErrorException {

        CompareNode local_compareNode = new CompareNode();
        MethodCallExpressionNode local_methodCallExpressionNode = new MethodCallExpressionNode();
        //(Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression)

//        if(parseMethodCallExpression().isPresent()){
//            return local_methodCallExpressionNode;
//        }else{

            local_compareNode.left = parseExpresion();
            if (tokens_list.peek().isPresent()){

                if(tokens_list.matchAndRemove(Token.TokenTypes.EQUAL).isPresent()){
                    //tokens_list.peek().get().getType() == Token.TokenTypes.EQUAL
                    // ==
                    local_compareNode.op = CompareNode.CompareOperations.eq;
                    //right expression
                    local_compareNode.right = parseExpresion();
                    return local_compareNode;
                } else if (tokens_list.matchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()) {
                    //tokens_list.peek().get().getType() == Token.TokenTypes.NOTEQUAL
                    // !=
                    local_compareNode.op = CompareNode.CompareOperations.ne;
                    //right expression
                    local_compareNode.right = parseExpresion();
                    return local_compareNode;
                } else if (tokens_list.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL).isPresent()) {
                    //tokens_list.peek().get().getType() == Token.TokenTypes.GREATERTHANEQUAL
                    // >=
                    local_compareNode.op = CompareNode.CompareOperations.ge;
                    //right expression
                    local_compareNode.right = parseExpresion();
                    return local_compareNode;
                } else if (tokens_list.matchAndRemove(Token.TokenTypes.GREATERTHAN).isPresent()) {
                    //tokens_list.peek().get().getType() == Token.TokenTypes.GREATERTHAN
                    // >
                    local_compareNode.op = CompareNode.CompareOperations.gt;
                    //right expression
                    local_compareNode.right = parseExpresion();
                    return local_compareNode;
                } else if (tokens_list.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL).isPresent()) {
                    //tokens_list.peek().get().getType() == Token.TokenTypes.LESSTHANEQUAL
                    // <=
                    local_compareNode.op = CompareNode.CompareOperations.le;
                    //right expression
                    local_compareNode.right = parseExpresion();
                    return local_compareNode;
                } else if (tokens_list.matchAndRemove(Token.TokenTypes.LESSTHAN).isPresent()) {
                    //tokens_list.peek().get().getType() == Token.TokenTypes.LESSTHAN
                    // <
                    local_compareNode.op = CompareNode.CompareOperations.lt;
                    //right expression
                    local_compareNode.right = parseExpresion();
                    return local_compareNode;
                }
            }

        //}

        return local_compareNode.left;
    }



    //returning Variable reference as of now.
    /*VariableReferenceNode local_variableReferenceNode = new VariableReferenceNode();
        local_variableReferenceNode.name = parseVariableReference().toString();
        return local_variableReferenceNode;


        */

    //Expression = Term { ("+"|"-") Term }
    public ExpressionNode parseExpresion() throws SyntaxErrorException {


        ExpressionNode local_expression;
        local_expression  = parseTerm();
        do {

            if (tokens_list.matchAndRemove(Token.TokenTypes.PLUS).isPresent()){
                MathOpNode local_mathOpNode = new MathOpNode();

                local_mathOpNode.left = local_expression;
                local_mathOpNode.op = MathOpNode.MathOperations.add;
                local_mathOpNode.right = parseTerm();

                local_expression = local_mathOpNode;

            }else if (tokens_list.matchAndRemove(Token.TokenTypes.MINUS).isPresent()){

                MathOpNode local_mathOpNode = new MathOpNode();

                local_mathOpNode.left = local_expression;
                local_mathOpNode.op = MathOpNode.MathOperations.subtract;
                local_mathOpNode.right = parseTerm();

                local_expression = local_mathOpNode;


            }
        }while (tokens_list.peek().isPresent() &&
                (tokens_list.peek().get().getType() == Token.TokenTypes.PLUS || tokens_list.peek().get().getType() == Token.TokenTypes.MINUS));



        return local_expression;
    }

    //Term = Factor { ("*"|"/"|"%") Factor }
    public ExpressionNode parseTerm() throws SyntaxErrorException {

        ExpressionNode local_expression;
        local_expression  = parseFactor();
        do {


            if (tokens_list.matchAndRemove(Token.TokenTypes.TIMES).isPresent()){
                MathOpNode local_FactormathNode = new MathOpNode();
                local_FactormathNode.left = local_expression;

                local_FactormathNode.op = MathOpNode.MathOperations.multiply;
                local_FactormathNode.right = parseFactor();

                local_expression = local_FactormathNode;

            }else if (tokens_list.matchAndRemove(Token.TokenTypes.DIVIDE).isPresent()){
                MathOpNode local_FactormathNode = new MathOpNode();
                local_FactormathNode.left = local_expression;

                local_FactormathNode.op = MathOpNode.MathOperations.divide;
                local_FactormathNode.right = parseFactor();

                local_expression = local_FactormathNode;


            } else if (tokens_list.matchAndRemove(Token.TokenTypes.MODULO).isPresent()) {
                MathOpNode local_FactormathNode = new MathOpNode();
                local_FactormathNode.left = local_expression;

                local_FactormathNode.op = MathOpNode.MathOperations.modulo;
                local_FactormathNode.right = parseFactor();

                local_expression = local_FactormathNode;

            }


        }while (tokens_list.peek().isPresent() &&
                (tokens_list.peek().get().getType() == Token.TokenTypes.TIMES || tokens_list.peek().get().getType() == Token.TokenTypes.DIVIDE
                        || tokens_list.peek().get().getType() == Token.TokenTypes.MODULO));


        return local_expression;
    }


    /*
        Factor = NumberLiteral | VariableReference | "true" | "false" | StringLiteral | CharacterLiteral
            | MethodCallExpression | "(" Expression ")" | "new" Identifier "(" [Expression {"," Expression }]")"



    * */
    public ExpressionNode parseFactor() throws SyntaxErrorException {

        if (tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.NUMBER){

            Optional<Token> temp = tokens_list.matchAndRemove(Token.TokenTypes.NUMBER);
            NumericLiteralNode number = new NumericLiteralNode();

            number.value = Float.parseFloat(temp.get().getValue());

            return number;
        }else if(tokens_list.matchAndRemove(Token.TokenTypes.TRUE).isPresent()){
                //TRUE Boolean value
                BooleanLiteralNode bool_value = new BooleanLiteralNode(true);

                return bool_value;

        } else if (tokens_list.matchAndRemove(Token.TokenTypes.FALSE).isPresent()) {
                // FALSE Boolean Value
                BooleanLiteralNode bool_value = new BooleanLiteralNode(false);

                return bool_value;

        } else if (tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.QUOTEDSTRING) {
            // STRING literal
            Optional<Token> temp_quote = tokens_list.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);

            StringLiteralNode string_value = new StringLiteralNode();

            string_value.value = temp_quote.get().getValue();

            return string_value;
        }else if (tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.QUOTEDCHARACTER) {
            // Character literal
            Optional<Token> temp_quote_char = tokens_list.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);

            StringLiteralNode CHAR_value = new StringLiteralNode();

            CHAR_value.value = temp_quote_char.get().getValue();

            return CHAR_value;
        } else if (tokens_list.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            //"(" Expression ")"
            ExpressionNode expression = parseExpresion();

            if (tokens_list.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
                return expression;
            }else {
                throw new SyntaxErrorException("Right Paranthesis not present",  tokens_list.getCurrentLine(), tokens_list.getCurrentColumnNumber());
            }


        } else if (tokens_list.matchAndRemove(Token.TokenTypes.NEW).isPresent()) {
            // "new" Identifier "(" [Expression {"," Expression }]")"

            NewNode temp_new = new NewNode();
            String value = tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            temp_new.className = value;

            if (tokens_list.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {

                if (tokens_list.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
                   // break;
                    return temp_new;
                }

                do {

                    temp_new.parameters.add(parseExpresion());

                    if (tokens_list.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
                        break;
                    }

                }while (tokens_list.matchAndRemove(Token.TokenTypes.COMMA).isPresent());

                return temp_new;
            }else {
                throw new SyntaxErrorException("No Left Paran(new)",  tokens_list.getCurrentLine(), tokens_list.getCurrentColumnNumber());
            }

        } else {
            if (tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
                Optional<ExpressionNode>  temp_method =  parseMethodCallExpression();
                if (temp_method.isPresent()) {
                    return temp_method.get();
                }
            }else {
                ExpressionNode  temp_variable =  parseVariableReference();
                return temp_variable;
            }
        }


        return null;
    }

    public ExpressionNode parseVariableReference() throws SyntaxErrorException {
        VariableReferenceNode local_variableReferenceNode = new VariableReferenceNode();

        if(tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.WORD){
            local_variableReferenceNode.name = tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
        }else {
            throw new SyntaxErrorException("No Word Given ! (parseVariableReference)\n", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }

        return local_variableReferenceNode;
    }



    //returning empty as of now, will implement later.
    //MethodCallExpression = [Identifier "."] Identifier "(" [Expression {"," Expression }] ")"
    public Optional<ExpressionNode> parseMethodCallExpression() throws SyntaxErrorException {
        MethodCallExpressionNode local_methodCallExpressionNode = new MethodCallExpressionNode();
        if (tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.DOT)){
            local_methodCallExpressionNode.objectName = Optional.of(tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue());
            tokens_list.matchAndRemove(Token.TokenTypes.DOT);
        }else {
            local_methodCallExpressionNode.objectName = Optional.empty();
        }

        if (!tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.LPAREN)) {
            throw new SyntaxErrorException("Expected: Word,left_Paran, Got :  " + tokens_list.peek() + ", " + tokens_list.peek(1), tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }else {
            //method name
            local_methodCallExpressionNode.methodName = tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            tokens_list.matchAndRemove(Token.TokenTypes.LPAREN);
            //after left parentheses
            if (tokens_list.peek().isPresent() && tokens_list.peek().get().getType() == Token.TokenTypes.WORD) {
                do {

                    local_methodCallExpressionNode.parameters.add(parseExpresion());

                }while (tokens_list.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
            }
        }

        if (!tokens_list.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()) {
            throw new SyntaxErrorException("No Right Parentheses", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }

        return Optional.of(local_methodCallExpressionNode);
    }

    /*
    parser-2
    */
    /*================================================================================================================
                              INTERFACE , METHOD HEADER , VARIABLE DECLARATIONS
    ================================================================================================================*/
    // parseInterface.
    public InterfaceNode parseInterface() throws SyntaxErrorException {
        //local InterfaceNode type variable.
        InterfaceNode local_Interface = new InterfaceNode();
        // Not handling multiple Interfaces back to back.
        //Not eating up the "Interface token for the next Interface right after the first one."

        //while (!tokens_list.done() || tokens_list.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()){
            Optional<Token> token = tokens_list.matchAndRemove(Token.TokenTypes.WORD);
            if (token.isPresent()) {
                local_Interface.name = token.get().getValue();
                //checking for NEWLINE tokens inside RequireNewLine function.
                if(RequireNewLine()){
                    if(tokens_list.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
                        // word "(" is the start of any method.


                        while (!tokens_list.matchAndRemove(Token.TokenTypes.DEDENT).isPresent() && !tokens_list.done()){
                            local_Interface.methods.add(parseMethodHeader());
                        }

                        RequireNewLine();
                    }else{
                        throw new SyntaxErrorException("Should have Indent!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                    }
                }else{
                    throw new SyntaxErrorException("Should have NewLine!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                }
            }else{
                throw new SyntaxErrorException("Should have word after Interface!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
            }

       // }
        return local_Interface;


    }



    //Identifier "(" VariableDeclarations ")" [ ":" VariableDeclaration { ","VariableDeclaration }]
    public MethodHeaderNode parseMethodHeader() throws SyntaxErrorException {
        MethodHeaderNode local_MethodHeader = new MethodHeaderNode();
        //tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN));

            //tokens_list.matchAndRemove(Token.TokenTypes.WORD)
            if(tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){

                //checking if word/name is given for method declaration.
                Optional<Token> token = tokens_list.matchAndRemove(Token.TokenTypes.WORD);
                if (token.isPresent()) {
                    local_MethodHeader.name = token.get().getValue();
                }

                //throwing error if there's no left parentheses after word.
                if (!tokens_list.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()){
                    throw new SyntaxErrorException("Should have Left parentheses !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                }
                // handling all the variables given between method parentheses.
                while (!tokens_list.matchAndRemove(Token.TokenTypes.RPAREN).isPresent() || tokens_list.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                    local_MethodHeader.parameters.add(parseVariableDeclarations());
                }

                //after handling the parentheses
                //tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()
                if(!RequireNewLine()){
                    if(tokens_list.matchAndRemove(Token.TokenTypes.COLON).isPresent()){
                        //looping till I hit a newLine. or until I'm at the end of list (or file).
                        //(!tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent() && !tokens_list.done())  ||
                        do {
                                local_MethodHeader.returns.add(parseVariableDeclarations());
                                if (tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
                                    break;
                                }

                        }while (tokens_list.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
                    }else{
                        throw new SyntaxErrorException("Give Colons Bro !!", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
                    }
                }

            }else{
                throw new SyntaxErrorException("wrong syntax for method Header! No NewLine.", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
            }




        return local_MethodHeader;
    }


    public VariableDeclarationNode parseVariableDeclarations() throws SyntaxErrorException {
        VariableDeclarationNode local_VariableDeclaration = new VariableDeclarationNode();

        if(isValidVariableDeclaration()){
            //giving variable type and name
            local_VariableDeclaration.type = tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            local_VariableDeclaration.name = tokens_list.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
        }else {
            //throwing error for not giving valid variable declaration.
            throw new SyntaxErrorException("Invalid Variable Declaration !", tokens_list.getCurrentLine(),tokens_list.getCurrentColumnNumber());
        }
        return local_VariableDeclaration;
    }

    public boolean isValidVariableDeclaration(){
        return tokens_list.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD);
    }


    /*
        ========================================================
            PARSING UP ALL THE POTENTIAL NEW LINES
        ========================================================
  */
    // bool RequireNewLine()
    public boolean RequireNewLine(){
        //checking is new line token is present in the first place
        if (tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            while (tokens_list.peek().get().getType() == Token.TokenTypes.NEWLINE){
                //simply eating up all the newline tokens.
                tokens_list.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
        }else {
            return false;
        }

        return true;
    }
}