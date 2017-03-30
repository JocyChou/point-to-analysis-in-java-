import soot.options.*;
import soot.*;
import soot.jimple.*;
import soot.toolkits.scalar.*;
import soot.util.*;
import soot.toolkits.graph.*;
import java.util.*;

class ClassTypeNode {
	String classType;

	public ClassTypeNode(String classType) {
		this.classType = classType;
	}
}

class Node {
	String name;
	List<ClassTypeNode> next;
	Set<ClassTypeNode> set;

	public Node(String name) {
		this.name = name;
		this.next = new ArrayList<ClassTypeNode>();
		this.set = new HashSet<ClassTypeNode>();
	}
}

class PointerGraph {
	List<Node> pointers;

	public PointerGraph() {
		this.pointers = new LinkedList<Node>();
	}
}

public class MyTransform extends BodyTransformer
{
	
	Map<Local, List<String>> localTypeMap = new HashMap<Local, List<String>>();
	Set<String> arrSet = new HashSet<String>();
	Map<String, ClassTypeNode> classType = new HashMap<String, ClassTypeNode>();
	

    protected void internalTransform(Body b, String phaseName, Map options)
    {
    	
    	Set<String> existedPointer = new HashSet<String>();
    	Map<String, Node> pointerToNode = new HashMap<String, Node>();
    	Set<String> classNodeName = new HashSet<String>();

    	Map<String, Node> basicPointer = new HashMap<String, Node>();

    	System.out.println("----------------------------------------");
    	Iterator it = b.getUnits().snapshotIterator();
    	while (it.hasNext()) {
    		Stmt s = (Stmt) it.next();
    		Unit u = (Unit) s;
    		if (s instanceof AssignStmt) {

    			//System.out.println(s);
    			String tmp = s.toString();
    			String[] t = tmp.split(" ");
    			//System.out.println(t[2] + " , " + t[t.length-1]);

    			// Pointer to a New Object
    			if (t[2].equals("new")) {
    				//System.out.println("In New");
    				//System.out.println(t[2] + " , " + t[t.length-1]);

    				String classTypeName = t[t.length-1] + "1";
    				int i=2;
    				while (classNodeName.contains(classTypeName)) {
    					classTypeName = t[t.length-1] + "" + i;
    					i += 1;
    				}
    				//System.out.println(classTypeName);
    				classNodeName.add(classTypeName);

    				ClassTypeNode thisClassNode = new ClassTypeNode(classTypeName);
    				String thisPointer = t[0];
    				if (pointerToNode.containsKey(thisPointer)) {
    					Node thisNode = pointerToNode.get(thisPointer);
    					if (thisNode.set.add(thisClassNode)) {
    						thisNode.next.add(thisClassNode);
    					}
    					pointerToNode.put(thisPointer, thisNode);
    					existedPointer.add(thisPointer);
    				}
    				else {
    					Node thisNode = new Node(thisPointer);
    					if (thisNode.set.add(thisClassNode)) {
    						thisNode.next.add(thisClassNode);
    					}
    					pointerToNode.put(thisPointer, thisNode);
    					existedPointer.add(thisPointer);
    				}
    			}

    			// Pointer Equals an Existed One
    			else if (existedPointer.contains(t[t.length-1])) {
    				//System.out.println("In Point To");
    				//System.out.println(t[2] + " , " + t[t.length-1]);

    				String thisPointer = t[0];
    				Node pointTo = pointerToNode.get(t[t.length-1]);

    				if (pointerToNode.containsKey(thisPointer)) {
    					Node thisNode = pointerToNode.get(thisPointer);
    					for (ClassTypeNode thisClassNode : pointTo.next) {
    						if (thisNode.set.add(thisClassNode)) {
    							thisNode.next.add(thisClassNode);
    						}
    					}
    					pointerToNode.put(thisPointer, thisNode);
    					existedPointer.add(thisPointer);
    				}
    				else {
    					Node thisNode = new Node(thisPointer);
    					for (ClassTypeNode thisClassNode : pointTo.next) {
    						if (thisNode.set.add(thisClassNode)) {
    							thisNode.next.add(thisClassNode);
    						}
    					}
    					pointerToNode.put(thisPointer, thisNode);
    					existedPointer.add(thisPointer);
    				}
    			}

    			// Basic Type Pointer
    			else if (!s.containsArrayRef()) {}
    		}
    	}
    	System.out.println("----------------------------------------");

    	for(String key: pointerToNode.keySet()) {
    		if (key.startsWith("$")) {
    			continue;
    		}
    		System.out.println("Object Pointer Name: " + key);
    		System.out.print(pointerToNode.get(key).name + " --> ");
    		for (ClassTypeNode classNode: pointerToNode.get(key).next) {
    			System.out.print(classNode.classType);
    		}
    		System.out.println("\n");
    	}
    	//System.out.println("Pointer: p  -->  [Point1]");
    	//System.out.println("Pointer: q  -->  [Point1]");
    	//System.out.println("Pointer: l1  -->  [Line1]");
    	//System.out.println("Pointer: l2  -->  [Line1]");
    	//System.out.println("Pointer: l3  -->  [Line2]");
    	//System.out.println("Pointer: t  -->  [Point2, Line3]");
    	System.out.println("----------------------------------------");
    	
    	/*
    	// Deal With the Normal Pointers - (To the basic type, which we don't need here)
    	System.out.println("----------------------------------------");
    	Iterator stmtIt = b.getUnits().snapshotIterator();
    	while (stmtIt.hasNext()) {
    		Stmt st = (Stmt) stmtIt.next();
    		Unit unit = (Unit) st;
    		//System.out.println(st.toString());
    		if (st instanceof AssignStmt) {

    			//Else
    			if (!s.containsArrayRef()) {

    				List<ValueBox> defBoxes = unit.getDefBoxes();
    				ValueBox defBox = defBoxes.get(0);
    				Value local = defBox.getValue();
    				Local l = (Local) local;
    				//System.out.println(local.toString());
    				List<ValueBox> useBoxes = unit.getUseBoxes();
    				Value use = useBoxes.get(0).getValue();

    				if (use instanceof Local) {
    					//System.out.println("right is local");
    					if (this.localTypeMap.containsKey(l)) {
    						if (this.localTypeMap.containsKey((Local) use)) {
    							for (String tmp: this.localTypeMap.get((Local) use)) {
    								this.localTypeMap.get(l).add(tmp);
    							}
    						}
    						//this.localTypeMap.get(l).add(use.toString());
    					}
    					else {
    						List<String> list = new ArrayList<String>();
    						if (this.localTypeMap.containsKey((Local) use)) {
    							for (String tmp: this.localTypeMap.get((Local) use)) {
    								list.add(tmp);
    							}
    						}
    						//list.add(use.toString());
    						this.localTypeMap.put(l, list);
    					}
    				}
    				else {
    					Type t = use.getType();
    			    	//System.out.println(t.toString());
    			    	//System.out.println(use.toString());
    			    	if (this.localTypeMap.containsKey(l)) {
    			    		this.localTypeMap.get(l).add(t.toString());
    			    	}
    			    	else {
    			    		List<String> list = new ArrayList<String>();
    			    		list.add(t.toString());
    			    		this.localTypeMap.put(l, list);
    			    	}
  
    				}
    				if (l.toString().startsWith("$")) {
    					this.localTypeMap.get(l).set(0, "Array[" + localTypeMap.get(l).get(0) + "]");
    				}
    			}
    		}
    	}
    	for (Local key: this.localTypeMap.keySet()) {
    		System.out.println(key + " --> " + this.localTypeMap.get(key));
    	}
    	System.out.println("----------------------------------------");
    	*/
    }
}