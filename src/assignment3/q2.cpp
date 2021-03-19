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
        int color;
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

        void printadj()
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
            omp_set_lock(&(this->objLock));
            int c = this->color;
            omp_unset_lock(&(this->objLock));
            return c;
        }

        void setColor(int color)
        {
            omp_set_lock(&(this->objLock));
            this->color = color;
            omp_unset_lock(&(this->objLock));
        }
        bool adjContains(Node *pNode)
        {
            vector<Node*>::iterator it = find(adj.begin(), adj.end(), pNode);
            return it != adj.end();
        }

    private:
        vector<Node*> adj = {};
        int id;
        // making a lock
        omp_lock_t objLock;
};

class Graph
{
    public:
        Graph(int numNodes);
        ~Graph();
        // Here we make the parallelizable functions
        void printGraph();
        void color();
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

// Do not need this function
void Graph::printGraph()
{
    map<string, int> strMap;
    for (Node *node: nodes)
    {
        cout << node->getId() << endl;
        vector<Node*> adjNodes = node->getAdj();
        cout << node->getId() << " adjency list length: " << adjNodes.size() << endl;
        cout << node->getId() << " adj: ";
        node->printadj();
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

void Graph::color()
{
    // All nodes are conflicting right now.
    vector<Node*> conflicts(nodes);
    while (conflicts.size() != 0 )
    {
        this->assign();
        conflicts = this->detectConflict();
    }
}

void Graph::assign()
{
    // Doing nothing useful
    #pragma omp parallel for
    for (int i=0;i<20;i++) {
        printf("Iteration %d done by thread %d accessing node %d\n",
               i,
               omp_get_thread_num(),
               nodes.at(i%10)->getId());
    }
}

vector<Node*> Graph::detectConflict()
{
    return vector<Node*> {};
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
    Graph aGraph(10);
    aGraph.printGraph();
    omp_set_num_threads(t);
    int i = 0;
    aGraph.color();
    cout << "Done the main" << endl;
}