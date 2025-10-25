package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {
    private TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */


    public Interpreter(TranNode top) {
        this.top = top;

        // Added the in-built Tran function console.write().
        ConsoleWrite console_1 = new ConsoleWrite();
        console_1.isVariadic = true;
        console_1.isShared = true;
        console_1.name = "write";

        ClassNode console = new ClassNode();
        console.name = "console";
        console.methods.add(console_1);

        top.Classes.add(console);
    }


    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {

        // Using Nested loop, loop through every class in tran node
        // and find a class who has a method whose name is "start" and isShared is true.

        //Iterating on every class that Tran node has
        for(ClassNode classes : top.Classes) {
            //Iterating on every method a particular class node has.
            for(MethodDeclarationNode method : classes.methods) {
                //Checking if a method is "isShared" and named start
                if (method.name.equals("start") && method.isShared ) {
                    //is not private and has no parameters
                    if (!method.isPrivate && method.parameters.isEmpty()) {
                        // calling interpretMethodCall on the found method
                        // that matches our requirements
                        interpretMethodCall(Optional.empty(), method, new LinkedList<>());
                        return; // exit after interpreting the start() method.
                    }
                }
            }
        }
        //Throw an exception if no such method exists.
        throw new IllegalArgumentException("No 'start' exists");

    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> result = new ArrayList<InterpreterDataType>();

        if(mc.objectName.isPresent()) { // this is shared method or a method from another class.
            Optional<ClassNode> other_class;
            if(locals.get(mc.objectName.get()) instanceof ReferenceIDT) {
                ReferenceIDT ref = (ReferenceIDT)locals.get(mc.objectName.get());
                other_class = Optional.of(ref.refersTo.get().astNode);


            }else {
                other_class = getClassByName(mc.objectName.get());
            }


            if(other_class.isPresent()) {

                for (MethodDeclarationNode method : other_class.get().methods){
                    // Get parameters (InterpreterDataType) for mc (MethodCallStatementNode)
                    // check using doesMatch function for each method declaration.

                    List<InterpreterDataType> parameters = getParameters(object,locals,mc);

                    if (doesMatch( method, mc, parameters)){

                        if((locals.containsKey(mc.objectName.get()))){
                            ReferenceIDT ref =  (ReferenceIDT)locals.get(mc.objectName.get());

                            result = interpretMethodCall( ref.refersTo ,method,parameters);
                        }else{
                            result = interpretMethodCall( Optional.empty() ,method,parameters);
                        }

                    }
                }
            }
        }else { // This method local to this class.
            ClassNode local_class = object.get().astNode;
            // Running for loop to check every method we have in our local class.
            for (MethodDeclarationNode local_method : local_class.methods){
                List<InterpreterDataType> local_parameters = getParameters(object,locals,mc);

                if (mc.returnValues.isEmpty()){
                    if (!local_method.returns.isEmpty()){
                        for (VariableDeclarationNode return_variable : local_method.returns){
                            VariableReferenceNode var_ref = new VariableReferenceNode();
                            var_ref.name = return_variable.name;
                            mc.returnValues.add(var_ref);
                        }

                    }
                    for (VariableDeclarationNode Variable : local_method.returns){
                        object.get().members.put(Variable.name, instantiate(Variable.type));
                    }
                }


                if (doesMatch(local_method, mc, local_parameters)){
                    result = interpretMethodCall(object, local_method, local_parameters);
                }
//                else{
//                    throw new RuntimeException("Does not match (Method call doesn't match Know method.)" + mc.methodName);
//                }
            }
        }
        return result;
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        List<InterpreterDataType> retVal = new ArrayList<InterpreterDataType>();

        if(m instanceof BuiltInMethodDeclarationNode){ // It's a Built-in method.
            BuiltInMethodDeclarationNode method = (BuiltInMethodDeclarationNode) m;
            retVal =  method.Execute(values);
            return retVal;
        }

        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        for (VariableDeclarationNode var : m.locals){

            locals.put(var.name , instantiate(var.type));
        }

        if (m.parameters.size() != values.size()){
            throw new IllegalArgumentException("Expected Right Amount of parameters. Need : " + m.parameters.size() + ", Got" + values.size());
        }

        for (int i = 0; i < values.size(); i++){
            InterpreterDataType value = values.get(i);
            locals.put(m.parameters.get(i).name, value);
        }
        if (object.isPresent()){
            for(String key : object.get().members.keySet()){
                if (!locals.containsKey(key)){
                    locals.put(key, object.get().members.get(key));
                }
            }
        }


        // Locals of object.
        if (object.isPresent()) {
            ClassNode cls = object.get().astNode;
            for (MemberNode mem : cls.members){
                VariableDeclarationNode var = mem.declaration;

//                locals.put(var.name , instantiate(var.type));
                locals.put(var.name , object.get().members.get(var.name));
            }

            interpretStatementBlock(object,m.statements,locals); //Interpreting the statement block.
        } else {
            interpretStatementBlock(object,m.statements,locals); //Interpreting the start() statement block
        }

        for (int i = 0; i < m.returns.size(); i++){

            if (locals.containsKey(m.returns.get(i).name)){
                retVal.add(locals.get(m.returns.get(i).name));
            }
        }

        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        // Get the parameters for the constructor call
        List<InterpreterDataType> parameters = getParameters(callerObj,locals,mc);

        //get the class for the constructor.

        Optional<ClassNode> class_call = getClassByName(newOne.astNode.name);
        if (class_call.isEmpty()){
            throw new IllegalArgumentException("Class not found");
        }

        //Finding a constructor that as a good match.
        ConstructorNode matching_constructor = null;
        for (ConstructorNode constructor : class_call.get().constructors){
            if (doesConstructorMatch(constructor,mc,parameters)){
                matching_constructor = constructor;
                break;
            }
        }

        if (matching_constructor == null){
            throw new IllegalArgumentException("Constructor not found for class : " + newOne.astNode.name);
        }

        // Run the constructor logic.
        interpretConstructorCall(newOne,matching_constructor,parameters);
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        HashMap<String, InterpreterDataType> locals = new HashMap<>();

        if (c.parameters.size() != values.size()){
            throw new IllegalArgumentException("Parameter count mismatch for constructor: Expected " + c.parameters.size() + ", but got " + values.size());
        }

        // adding the local constructor variables.
        for (VariableDeclarationNode var : c.locals){
            locals.put(var.name , instantiate(var.type));
        }
        // Step 3: Add parameters to local variables
        for (int i = 0; i < c.parameters.size(); i++) {

            String paramName = c.parameters.get(i).name;
            locals.put(paramName, values.get(i));
        }

        // Locals of object.
        ClassNode cls = object.astNode;
        for (MemberNode mem : cls.members){
            VariableDeclarationNode var = mem.declaration;
            locals.put(var.name , instantiate(var.type));
            object.members.put(var.name , locals.get(var.name));
        }

        // Execute the constructor's block
        interpretStatementBlock(Optional.of(object),c.statements,locals);
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do every statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call findMethodForMethodCallAndRunIt(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     *
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {

        for(StatementNode statement : statements){
            //For AssignmentNode
            if (statement instanceof AssignmentNode assign){
                if (assign.target instanceof VariableReferenceNode){
                    //target and expression for the given target_variable reference node.
                    String target_variable = ((VariableReferenceNode) assign.target).name;
                    ExpressionNode expression = assign.expression;
                    //Using FindVariable() to get the target.
                    InterpreterDataType target = findVariable(target_variable,locals,object);
                    // Use Evaluate() function on the expression.
                    InterpreterDataType expression_value = evaluate(locals,object,expression);

                    target.Assign(expression_value); // Call Assign() on the target IDT. with the result of Evaluate().

                } else {
                    throw new IllegalArgumentException("Variable reference expected");
                }
            } //For MethodCallStatementNode.
            else if (statement instanceof MethodCallStatementNode MethodCall){
                //call findMethodForMethodCallAndRunIt().
                List<InterpreterDataType> returned_values = findMethodForMethodCallAndRunIt(object,locals,MethodCall);

                    if(MethodCall.returnValues.size() != returned_values.size()){
                        throw new IllegalArgumentException("Number of return values mismatch (interpreter 298.)");
                    }

                    //Loop over the returned values and copy the into our local variables
                // x,y,z = foo()
                    for(int i=0; i < returned_values.size(); i++){
                        VariableReferenceNode local_variable = MethodCall.returnValues.get(i);
                        //replacing the value of the variable in Hashmap with the new returned one.
                        locals.replace(local_variable.name, returned_values.get(i));
                    }
            } //For If.

            else if (statement instanceof IfNode if_node) {

                //Evaluate() the condition.
                BooleanIDT condition_IDT = (BooleanIDT) evaluate(locals,object,if_node.condition);

                //If condition is true, InterpretStatementBlock() on the if's statements.
                if(condition_IDT.Value){
                    interpretStatementBlock(object,if_node.statements,locals);
                } else if (!condition_IDT.Value && if_node.elseStatement.isPresent()) {
                    // If condition is not true AND there is an else statement,
                    // InterpretStatementBlock on the else body.
                    ElseNode elseNode = if_node.elseStatement.get();
                    interpretStatementBlock(object,elseNode.statements,locals);
                }
            }
            else if (statement instanceof LoopNode loop) {
                /*
                * Loop:
                 *          While we are not done:
                 *              */
                BooleanIDT condition;
                do {
                    //if this is a boolean loop, Evaluate() to get true or false.
                    condition = (BooleanIDT) evaluate(locals,object,loop.expression);

                    if (loop.assignment.isPresent()){
                        loop.assignment.get().name = condition.toString();
                    }
                    // If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
                    // Break of the condition is false.
                    if (!condition.Value){
                        break;
                    }else {
                        //execute the body of loop.
                        interpretStatementBlock(object,loop.statements,locals);
                    }

                }while (condition.Value);
            }

        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call findMethodForMethodCallAndRunIt() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {

        if(expression instanceof BooleanLiteralNode){ //  BooleanLiteralNode.
            BooleanLiteralNode b = (BooleanLiteralNode) expression;
            return new BooleanIDT(b.value);
        } else if (expression instanceof  NumericLiteralNode) { // NumericLiteralNode
            NumericLiteralNode n = (NumericLiteralNode) expression;
            return new NumberIDT(n.value);
        } else if (expression instanceof  StringLiteralNode) { //StringLiteralNode
            StringLiteralNode s = (StringLiteralNode) expression;
            return new StringIDT(s.value);
        } else if (expression instanceof CharLiteralNode) { // CharLiteralNode
            CharLiteralNode c = (CharLiteralNode) expression;
            return new CharIDT(c.value);
        } else if (expression instanceof BooleanOpNode){ // Boolean , BooleanOpNode

            BooleanOpNode b = (BooleanOpNode) expression;
            //Evaluate left and right.
            BooleanIDT b_left = (BooleanIDT) evaluate(locals,object, b.left);
            BooleanIDT b_right = (BooleanIDT) evaluate(locals,object, b.right);
            //performing and/or operation after checking weather the operation is and or not.
            boolean result = (b.op == BooleanOpNode.BooleanOperations.and)? b_left.Value && b_right.Value : b_left.Value || b_right.Value;
            return new BooleanIDT(result);
        } else if (expression instanceof CompareNode) { // comparison , CompareNodes
            CompareNode compare = (CompareNode) expression;
            InterpreterDataType left = evaluate(locals,object,compare.left);
            InterpreterDataType right = evaluate(locals,object,compare.right);

            // performing all the possible comparison operations.
            boolean result;
            if (left instanceof NumberIDT && right instanceof NumberIDT) { // Comparing NumberIDTs
                //comparing numbers.
                float left_value = ((NumberIDT) left).Value;
                float right_value = ((NumberIDT) right).Value;

                result = switch (compare.op){
                    case lt -> left_value < right_value;
                    case le -> left_value <= right_value;
                    case gt -> left_value > right_value;
                    case ge -> left_value >= right_value;
                    case eq -> left_value == right_value;
                    case ne -> left_value != right_value;
                };

            } else if (left instanceof StringIDT && right instanceof StringIDT) { //Comparing StringIDTs
                // Comparing strings
                String left_value = ((StringIDT) left).Value;
                String right_value = ((StringIDT) right).Value;

                result = switch (compare.op){
                    case eq -> left_value.equals(right_value);
                    case ne -> !left_value.equals(right_value);
                    default -> throw new IllegalArgumentException("Unexpected Operation on String : " + compare.op);
                };
            } else if (left instanceof CharIDT && right instanceof CharIDT) { //Comparing CharIDTs
                // if we have Characters
                char left_value = ((CharIDT) left).Value;
                char right_value = ((CharIDT) right).Value;

                result = switch (compare.op){
                    case eq -> left_value == right_value;
                    case ne -> left_value != right_value;
                    default -> throw new IllegalArgumentException("Unexpected Operation on Char : " + compare.op);
                };
            } else if (left instanceof BooleanIDT && right instanceof BooleanIDT) { // Comparing BooleanIDTs
                // If we have a Boolean nodes
                boolean left_value = ((BooleanIDT) left).Value;
                boolean right_value = ((BooleanIDT) right).Value;

                result = switch (compare.op){
                    case eq -> left_value == right_value;
                    case ne -> left_value != right_value;
                    default -> throw new IllegalArgumentException("Unexpected Operation on Boolean : " + compare.op);
                };
            } else {
                throw new IllegalArgumentException("Unsupported comparison types: " + left.getClass() + " and " + right.getClass());
            }
            return new BooleanIDT(result);
        } else if (expression instanceof MathOpNode) {
            MathOpNode math = (MathOpNode) expression;
            // Evaluating the left and right side of the MathOpNode.
            InterpreterDataType left = evaluate(locals,object,math.left);
            InterpreterDataType right = evaluate(locals,object,math.right);

            // Performing operations on Number and String.
            if (left instanceof NumberIDT && right instanceof NumberIDT) {
                float left_value = ((NumberIDT) left).Value;
                float right_value = ((NumberIDT) right).Value;

                // performing the math operations.
                float result = switch (math.op){
                    case add -> left_value + right_value;
                    case subtract -> left_value - right_value;
                    case multiply -> left_value * right_value;
                    case divide -> {
                        if (right_value == 0.0) {
                            throw new IllegalArgumentException("Division by zero");
                        }
                        yield left_value / right_value;
                    }
                    case modulo -> {
                        if (right_value == 0.0) {
                            throw new IllegalArgumentException("Modulo by zero");
                        }
                        yield left_value % right_value;
                    }

                };

                return new NumberIDT(result);
            } else if (left instanceof StringIDT && right instanceof StringIDT) {
                String left_value = ((StringIDT) left).Value;
                String right_value = ((StringIDT) right).Value;

                if (math.op == MathOpNode.MathOperations.add){
                    return new StringIDT(left_value + right_value);
                }else {
                    throw new IllegalArgumentException("Unsupported Operation on String : " + math.op);
                }
            } else {
                throw new IllegalArgumentException("Unsupported types for MathOp: " + left.getClass() + " and " + right.getClass());
            }

        } else if (expression instanceof MethodCallExpressionNode) {
            MethodCallExpressionNode mehtodCall = (MethodCallExpressionNode) expression;
            MethodCallStatementNode mc = new MethodCallStatementNode(mehtodCall);

            List<InterpreterDataType> result = findMethodForMethodCallAndRunIt(object,locals,mc);
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Method call did not return any result " + mc);
            }
            return result.getFirst(); // returning the first result.
        } else if (expression instanceof VariableReferenceNode) {

            VariableReferenceNode vref = (VariableReferenceNode) expression;
            return findVariable(vref.name,locals,object);

        } else if (expression instanceof NewNode) {
            NewNode newNode = (NewNode) expression;
            List<InterpreterDataType> param_list = new ArrayList<InterpreterDataType>();

            for (ExpressionNode parameter : newNode.parameters){
                param_list.add(evaluate(locals,object,parameter));
            }

            MethodCallStatementNode mc = new MethodCallStatementNode();
            mc.methodName = newNode.className;
            mc.parameters = newNode.parameters;

            InterpreterDataType objectNew = instantiate(newNode.className);
            if (objectNew instanceof ReferenceIDT ref){
                Optional<ClassNode> temp_class = getClassByName(newNode.className);
                if (temp_class.isPresent()){
                    ObjectIDT temp_obj = new ObjectIDT(temp_class.get());
                    findConstructorAndRunIt(object,locals,mc, temp_obj);
                    ref.refersTo = Optional.of(temp_obj);
                }else {
                    throw new RuntimeException("Class not found (Interpreter 536)");
                }

            }else {
                findConstructorAndRunIt(object,locals,mc, (ObjectIDT) objectNew);
            }

            return objectNew;

        } else if (expression instanceof NotOpNode) {
            NotOpNode notOp = (NotOpNode) expression;

            InterpreterDataType type = evaluate(locals,object,notOp.left);

            //ensuring that operand is a boolean.
            if(!(type instanceof BooleanIDT)) {
                throw new IllegalArgumentException("Unsupported Operation on NotOp : " + type.getClass());
            }

            // Negating the boolean value
            boolean result = !((BooleanIDT) type).Value;

            return new BooleanIDT(result);
        }
        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this method call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        if(m instanceof BuiltInMethodDeclarationNode){
            BuiltInMethodDeclarationNode built_in_m = (BuiltInMethodDeclarationNode)m;
            if(built_in_m.isVariadic){
                //if m is a built-in and isVariadic is true, just returning true.
                return true;
            }

        }


        // if the names don't match, we return false.
        if(!m.name.equals(mc.methodName)){
            return false;
        }



        // checking parameter counts (both declared count vs method call and declared count vs parameters list)
        if (m.parameters.size() != parameters.size()) {
            return false;
        }

        if (m.parameters.size() != mc.parameters.size()) {
            return false;
        }

        if (m.returns.size() != mc.returnValues.size() ) {
            return false;
        }

        for (int i = 0; i < parameters.size(); i++) {
            String expected_type = m.parameters.get(i).type;
            InterpreterDataType actual_type = parameters.get(i);

            //using typeMatchToIDT to check is types match for all of the parameters.
            if (!typeMatchToIDT(expected_type, actual_type)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        if (c.parameters.size() != parameters.size()) {
            return false;
        }

        if (c.parameters.size() != mc.parameters.size()) {
            return false;
        }

        for(int i = 0; i < parameters.size(); i++){
            String expected_type = c.parameters.get(i).type; //From VariableDeclarationNode
            InterpreterDataType actual_type = parameters.get(i);

            // using the typeMatchIDT function to check the types.
            if (!typeMatchToIDT(expected_type, actual_type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> result = new LinkedList<InterpreterDataType>();

        for (ExpressionNode parameter : mc.parameters) {
            InterpreterDataType Datatype = evaluate(locals, object, parameter);

            result.add(Datatype);
        }

        return result;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {

        if (idt == null) {
            return false; // Null IDT is invalid.
        }

        // for simple types.
        if (idt instanceof BooleanIDT) {
            if(!type.equals("boolean")){
                return false;
            }else {
                return true;
            }
        } else if (idt instanceof NumberIDT ) {
            if(!type.equals("number")){
                return false;
            }else {
                return true;
            }
        } else if (idt instanceof StringIDT) {
            if(!type.equals("string")){
                return false;
            }else {
                return true;
            }

        } else if (idt instanceof CharIDT) {
            if(!type.equals("character")){
                return false;
            }else {
                return true;
            }
        }

        if (idt instanceof ObjectIDT){
            ObjectIDT obj = (ObjectIDT) idt;
            ClassNode local_class = obj.astNode;
            // checking if the class name matches the type.
            if (local_class.name.equals(type)){
                return true;
            }

            //checking if any of the interface matches the type.
            for (String i : local_class.interfaces) {
                if (i.equals(type)) {
                    return true;
                }
            }

        }

        // check for reference types.
        if (idt instanceof ReferenceIDT){
            ReferenceIDT ref = (ReferenceIDT) idt;

            if (ref.refersTo.isPresent()){
                return typeMatchToIDT(type, ref.refersTo.get());
            }else {
                // cannot find the refers to value
                return false;
            }
        }

        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        ClassNode local_class = object.astNode;

        // loop through each method in class node.
        for(MethodDeclarationNode method : local_class.methods) {
            if(doesMatch(method, mc, parameters)) {
                return method;
            }
        }

        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        // looping through the list of classes in the main TranNode (top)
        // and checking if any of those classes match the given name.
        for(ClassNode classes : top.Classes){
            if(classes.name.equals(name)){
                return Optional.of(classes);
            }
        }
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {

        if(locals.containsKey(name)) {
            // if it's in the local variable list.
            return locals.get(name);
        } else if (object.isPresent() && object.get().members.containsKey(name)) {
            // if it's part of object member list.
            return object.get().members.get(name);
        } else {
            // if the variable is not found in any of those, throwing an error.
            throw new RuntimeException("Unable to find variable " + name);
        }

    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {

        if (type.equals("string")){
            //"" for string
            StringIDT temp_string = new StringIDT("");
            return temp_string;
        } else if (type.equals("number")) {
            //0 for number
            NumberIDT temp_num = new NumberIDT(0);
            return temp_num;
        } else if (type.equals("boolean")) {
            //false for boolean
            BooleanIDT temp_bool = new BooleanIDT(false);
            return temp_bool;
        } else if (type.equals("character")) {
            //' ' for character
            CharIDT temp_char = new CharIDT(' ');
            return temp_char;
        }
        return new ReferenceIDT();
    }
}
