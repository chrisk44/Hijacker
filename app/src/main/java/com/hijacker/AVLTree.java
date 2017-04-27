package com.hijacker;

/*
    Copyright (C) 2017  Christos Kyriakopoylos

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import android.util.Log;

class AVLTree<T>{
    public static final int LEFT_SON = 0, RIGHT_SON = 1;
    Node root;
    public AVLTree(){}
    void clear(){
        //Memory will be reclaimed by GC
        root = null;
    }
    T findById(long id){
        //Find an object with a specific id
        if(root==null) return null;     //Empty tree

        Node node = find(id, root);
        if(node==null) return null;     //This shouldn't happen

        if(node.id==id) return node.obj;
        return null;
    }
    private Node find(long id, Node node){
        //Find an object with a specific id below node
        if(node==null){
            Log.e("HIJACKER/AVL", "[E] find(" + id + ", NULL)");
            return null;
        }

        if(node.id==id) return node;      //We were looking for an object, no need to go lower
        else if(id > node.id){
            if(node.RS!=null) return find(id, node.RS);
            else return node;
        }else{
            if(node.LS!=null) return find(id, node.LS);
            else return node;
        }
    }
    void add(T newObj, long newID){
        //Insert a new Object in the tree
        if(root==null){
            root = new Node(newObj, newID, null);
            return;
        }
        Node node = find(newID, root);
        if(node==null){
            Log.e("HIJACKER/AVL", "[E] node in add() is null");
            return;
        }

        if(node.id==newID){
            Log.e("HIJACKER/AVL", "[E] Object with id " + newID + " already exists");
            return;
        }

        //node is the leaf under which the new object will be inserted
        Node newParent;
        if(newID > node.id){
            node.LS = new Node(node.obj, node.id, node);
            node.RS = new Node(newObj, newID, node);
            newParent = node;
        }else{
            Node new_node = new Node(newObj, newID, node.P);
            if(node.sonType()==LEFT_SON){
                //LS changed
                node.P.LS = new_node;
            }else if(node.sonType()==RIGHT_SON){
                //RS changed
                node.P.RS = new_node;
            }else{
                //node is AVLRoot
                this.root = new_node;
            }

            new_node.LS = new Node(newObj, newID, new_node);
            new_node.RS = node;
            node.P = new_node;

            newParent = new_node;
        }

        while(newParent!=null){
            newParent.recalc();
            if(newParent.balance < -1 || newParent.balance > 1){
                rebalance(newParent);
                break;
            }

            newParent = newParent.P;
        }
    }
    void print(Node node){
        if(node==null){
            Log.e("HIJACKER/AVL", "[E] Null node in print(node)");
            return;
        }

        String str = "[" + node.id + "(" + node.balance + "," + node.height + ")]";
        str += "(" + (node.LS==null ? "null" : node.LS.id + "(" + node.LS.balance + "," + node.LS.height + ")");
        str += "," + (node.RS==null ? "null" : node.RS.id + "(" + node.RS.balance + "," + node.RS.height + ")") + ")";
        System.out.println(str);
        if(node.LS!=null){
            print(node.LS);
        }
        if(node.RS!=null){
            print(node.RS);
        }
    }
    void printTree(Node root, int level){
        if(root==null) return;

        printTree(root.RS, level+1);
        if(level!=0){
            for(int i=0;i<level;i++){
                System.out.print("|\t");
            }

            System.out.println("|-------" + root.id);
        }else System.out.println(root.id);
        printTree(root.LS, level+1);
    }

    private void rebalance(Node root){
        //root is the root of the subtree that needs to be rebalanced
        if(root.balance==2){
            //Right-heavy
            if(root.RS.balance>0){
                rotateLeft(root, root.RS);
            }else{
                rotateRight(root.RS, root.RS.LS);
                rotateLeft(root, root.RS);
            }
        }else if(root.balance==-2){
            //Left-heavy
            if(root.LS.balance<0){
                rotateRight(root, root.LS);
            }else{
                rotateLeft(root.LS, root.LS.RS);
                rotateRight(root, root.LS);
            }
        }else{
            Log.e("HIJACKER/AVL", "[E] Node with id " + root.id + " has a balance of " + root.balance + ". No rebalance (shouldn't be here)");
        }
    }
    private void rotateLeft(Node root, Node pivot){
        int initRootType = root.sonType();

        root.RS = pivot.LS;
        root.RS.P = root;
        pivot.P = root.P;
        root.P = pivot;
        pivot.LS = root;

        if(initRootType==LEFT_SON){
            pivot.P.LS = pivot;
        }else if(initRootType==RIGHT_SON){
            pivot.P.RS = pivot;
        }else{
            //root was AVLRoot
            this.root = pivot;
        }

        root.recalc();
        pivot.recalc();
        if(pivot.P!=null) pivot.P.recalc();
    }
    private void rotateRight(Node root, Node pivot){
        int initRootType = root.sonType();

        root.LS = pivot.RS;
        root.LS.P = root;
        pivot.P = root.P;
        root.P = pivot;
        pivot.RS = root;

        if(initRootType==LEFT_SON){
            pivot.P.LS = pivot;
        }else if(initRootType==RIGHT_SON){
            pivot.P.RS = pivot;
        }else{
            //root was AVLRoot
            this.root = pivot;
        }

        root.recalc();
        pivot.recalc();
        if(pivot.P!=null) pivot.P.recalc();
    }
    private class Node{
        Node LS, RS, P;
        private int height = 0;
        int balance = 0;
        final long id;
        final T obj;
        Node(T obj, long id, Node P){
            this.obj = obj;
            this.id = id;
            this.P = P;
        }
        private void recalc(){
            height = 1 + (RS==null ? 0 : Math.max(LS.height, RS.height));
            balance = RS==null ? 0 : RS.height - LS.height;
        }
        int sonType(){
            if(P==null) return -1;

            if(this.equals(P.LS)) return LEFT_SON;
            else return RIGHT_SON;
        }
    }
}

