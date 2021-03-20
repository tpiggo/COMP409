#include <omp.h>
#include <iostream>
#include <vector>
#include <cstdlib>
#include <algorithm>
#include <string>
#include <sstream>
#include <time.h>
#include <map>
using namespace std;

class Node
{
    public:
        Node(int id)
        {
            this->id = id;
            this->color = 0;
            omp_init_lock(&(this->objLock));
        }

        ~Node()
        {
            omp_destroy_lock(&(this->objLock));
        }

        vector<Node *> getAdj() const
        {
            return adj;
        }

        void addNode(Node *pNode)
        {
            adj.push_back(pNode);
        }

        int getId() const
        {
            return id;
        }

        // Delete this 
        void printAdj()
        {
            for (Node *n: adj)
            {
                cout << n->getId();
                if (adj.back() != n)
                {
                    cout << ", ";
                }
            }
        }

        int getColor()
        {
            // may not need the lock
            omp_set_lock(&(this->objLock));
            int c = this->color;
            omp_unset_lock(&(this->objLock));
            return c;
        }

        void setColor(int color)
        {
            // may not need the lock
            omp_set_lock(&(this->objLock));
            this->color = color;
            omp_unset_lock(&(this->objLock));
        }
        bool adjContains(Node *pNode)
        {
            vector<Node*>::iterator it = find(adj.begin(), adj.end(), pNode);
            return it != adj.end();
        }

        void reorderAdj();

    private:
        vector<Node*> adj = {};
        int id;
        // making a lock
        omp_lock_t objLock;
        int color;
};

bool nodeComp(Node *n1, Node *n2)
{
    return n1->getColor() < n2->getColor();
}
// reorder the adjacency list in terms of color
void Node::reorderAdj()
{
    sort(this->adj.begin(), this->adj.end(), nodeComp);
}

class Graph
{
    public:
        Graph(int numNodes);
        ~Graph();
        // Here we make the parallelizable functions
        void printGraph();
        void color();
        // Delete this
        void checkGraph();
    private:
        vector<Node*> nodes = {};
        int numNodes;
        void assign();
        vector<Node*> detectConflict();
};

int a = 100;
Graph::Graph(int numNodes)
{
    for (int i = 1; i <= numNodes; i++)
    {
        //Node *aNode = new Node(i);
        Node *aNode= new Node(i);
        nodes.push_back(aNode);
    }
    this->numNodes = numNodes;
    srand(time(NULL) + a++);
    int maxEdges = ((this->numNodes - 1)*this->numNodes)/2;
    int numEdges = (int)(rand() % maxEdges)+1;
    cout << "number of edges: " << numEdges << endl;
    for (Node *node : nodes)
    {
        cout << node->getId() << endl;
    }
    for (int i = 0 ; i < numEdges; i++)
    {
        // Get the position in the ordered list of nodes
        int pN1 = (int)(rand() % this->numNodes);
        int pN2 = (int)(rand() % this->numNodes);
        Node *n1 = nodes.at(pN1);
        Node *n2 = nodes.at(pN2);
        while (pN1 == pN2 || n1->adjContains(n2) || n2->adjContains(n1))
        {
            // get the new random elements
            pN1 = (int)(rand() % this->numNodes);
            pN2 = (int)(rand() % this->numNodes);
            n1 = nodes.at(pN1);
            n2 = nodes.at(pN2);
        }
        n1->addNode(n2);
        n2->addNode(n1);
        cout << n1->getId() << " " << n2->getId() << endl;
    }
}

// Delete this function later
void Graph::checkGraph()
{
    map<string, int> strMap;
    for (Node *node: nodes)
    {
        vector<Node*> adjNodes = node->getAdj();
        cout << node->getId() << " adjency list length: " << adjNodes.size() << endl;
        cout << node->getId() << " adj: ";
        node->printAdj();
        cout << endl;
    }
    for (Node *node: nodes)
    {
        for (Node *adj: node->getAdj())
        {
            string s = "";
            if (adj->getId() > node->getId())
            {
                s += to_string(node->getId());
                s += ":";
                s += to_string(adj->getId());
            }
            else
            {
                s += to_string(adj->getId());
                s += ":";
                s += to_string(node->getId());
            }
            strMap[s] += 1;
        }
    }
    for (map<string, int>::iterator it = strMap.begin(); it != strMap.end(); it++)
    {
        cout << it->first << ", " << it->second << endl;
    }
}
// Do not need this function
void Graph::printGraph()
{
    map<string, int> strMap;
    for (Node *node: nodes)
    {
        vector<Node*> adjNodes = node->getAdj();
        cout << node->getId() << " adjency list length: " << adjNodes.size() << endl;
        cout << node->getId() << " with color: " << node->getColor() << " ; with adj: ";
        node->printAdj();
        cout << endl;
    }

}

void Graph::color()
{
    // All nodes are conflicting right now.
    vector<Node*> conflicts(nodes);
    int i = 0;
    while (conflicts.size() != 0 )
    {
        this->assign();
        conflicts = this->detectConflict();
        i++;
        if (i > 100 )
        {
            cout << "=============== You've entered an invalid state ==============" << endl;
            break;
        }
    }
}

void Graph::assign()
{
    // Doing nothing useful
    #pragma omp parallel for
    for (int i=0;i<nodes.size();i++) {
        // Each thread should check its neighbours and make a decision
        Node *node = nodes.at(i);
        int minC = 1; // Lowest possible color;
        for (Node *pNode : node->getAdj())
        {
            if (pNode->getColor() == minC)
            {
                minC++;
            }
        }
        node->setColor(minC);

    }
}

vector<Node*> Graph::detectConflict()
{
    vector<Node*> conflicts;
    #pragma omp parallel for
    for (int i=0;i<nodes.size(); i++) {
        // Each thread should check its neighbours and make a decision
        Node *node = nodes.at(i);
        node->reorderAdj();
        for (Node *pNode : node->getAdj())
        {
            if (pNode->getColor() == node->getColor())
            {
                #pragma omp critical
                {
                    conflicts.push_back(node);
                }
                // You're done
                break;
            }
        }
    }
    cout << "-------------- More than " << conflicts.size() << "nodes in CONFLICT ------------" << endl;

    return conflicts;
}

Graph::~Graph()
{
    while (nodes.size() !=0)
    {
        Node *back = nodes.at(nodes.size()-1);
        nodes.pop_back();
        delete back;
    }
}

// My Main function
int main()
{
    int t = 3;
    Graph aGraph(100);
    aGraph.printGraph();
    omp_set_num_threads(t);
    aGraph.color();
    cout << "-----------------" <<endl;
    aGraph.printGraph();
    cout << "Done the main" << endl;
}