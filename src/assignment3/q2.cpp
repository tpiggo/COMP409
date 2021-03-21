#include <omp.h>
#include <iostream>
#include <vector>
#include <cstdlib>
#include <algorithm>
#include <string>
#include <time.h>
#include <map>
#include <chrono>
#include <fstream>
using namespace std;

class Node
{
    public:
        Node(int id)
        {
            this->id = id;
            this->color = 0;
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
            int col = this->color;
            return col;
        }

        void setColor(int color)
        {
            // may not need the lock
            this->color = color;
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
        int color;
        omp_lock_t nLock;
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

class Edge
{
    public: 
        Node *n1;
        Node *n2;
        Edge(Node *n1, Node *n2)
        {
            this->n1 = n1;
            this->n2 = n2;
        }
};


class Graph
{
    public:
        Graph(int numNodes, int edges);
        ~Graph();
        void printGraph();
        // Calling the parallelizable functions here.
        // Wrapping them up in this easy to use function for data hiding.
        void color();
        int getMinColoring();
        void inspectGraph();

        // Delete this
        void checkGraph();
    private:
        vector<Node*> nodes = {};
        vector<Edge*> edges = {};
        int numNodes;        
        // Here we make the parallelizable functions
        void assign();
        vector<Node*> detectConflict();
};

int a = 1;
Graph::Graph(int numNodes, int numEdges)
{
    for (int i = 1; i <= numNodes; i++)
    {
        nodes.push_back(new Node(i));
    }

    this->numNodes = numNodes;
    srand(time(NULL)+a);
    rand();
    cout << "number of edges: " << numEdges << endl;
    // for (Node *node : nodes)
    // {
    //     cout << node->getId() << endl;
    // }
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
        edges.push_back(new Edge(n1,n2));
        // cout << n1->getId() << " " << n2->getId() << endl;
    }
}

Graph::~Graph()
{
    while (this->edges.size() != 0)
    {
        Edge *back = edges.at(edges.size()-1);
        edges.pop_back();
        delete back;
    }

    while (this->nodes.size() !=0)
    {
        Node *back = nodes.at(nodes.size()-1);
        nodes.pop_back();
        delete back;
    }
}

// Delete this function later
void Graph::checkGraph()
{
    ofstream mFile;
    mFile.open("graphout.txt");
    for (Edge *e: this->edges)
    {
        //cout << e->n1->getId() << " : " << e->n2->getId() << " -> " << e->n1->getColor() << " : " << e->n2->getColor() << endl;
        
        mFile << e->n1->getId() << " : " << e->n2->getId() << " -> " << e->n1->getColor() << " : " << e->n2->getColor() << "\n";
    }
    for (Node *n : this->nodes)
    {
        // cout << n->getId() << endl;
        mFile <<  n->getId() << "\n";
    }
    for (Edge *e: this->edges)
    {
        // cout << e->n1->getId() << " " << e->n2->getId() << endl;
        mFile << e->n1->getId() << " " << e->n2->getId() << "\n";
    }
    mFile.close();
}

void Graph::inspectGraph()
{
    bool isInvalid = false;
    for (Node *node: this->nodes)
    {
        for (Node *adj: node->getAdj())
        {
            if (adj->getColor() == node->getColor())
            {
                isInvalid = true;
                cout << adj->getId() << " : " << node->getId() << " and both have the same colour, namely: " << node->getColor() << endl;
            }
        }
    }
    if (isInvalid)
    {
        cout << "You have a problem! Two nodes with an adacent node of the same colour" << endl;
    }
}

void Graph::printGraph()
{
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
        if (i > 1000 )
        {
            cout << "=============== You've entered an invalid state ==============" << endl;
            break;
        }
    }
}

int Graph::getMinColoring()
{
    int minC = 1;
    for (Node *n: this->nodes)
    {
        if (minC < n->getColor())
            minC = n->getColor();
    }
    return minC;
}

void Graph::assign()
{
    // Doing nothing useful
    #pragma omp parallel for
    for (int i=0; i < nodes.size(); i++) {
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
    cout << "-------------- More than " << conflicts.size() << " nodes in CONFLICT ------------" << endl;

    return conflicts;
}

// My Main function
int main(int argc, char *argv[])
{
    int t=1, n = 3, e = 1;
    if (argc>1) {
        n = atoi(argv[1]);
        printf("Using %d nodes\n",n);
        if (argc>2) {
            e = atoi(argv[2]);
            printf("Using %d edges\n",t);
            if (argc > 3)
            {
                t = atoi(argv[3]);
                printf("Using %d threads\n",t);
            }
        }
    }
    int maxEdges = ((n - 1)*n)/2;
    if (n < 3)
    {    
        cout << "Error: Not enough nodes!" << endl;
        return -1;
    }
    else if (e < 1)
    {
        cout << "Error: Not enough edges!" << endl;
        return -1;
    }
    else if (e > maxEdges)
    {
        cout << "Error: Exceeded the maximum number of edges: "<< maxEdges << "; edges: " << e << endl;
        return -1;
    }

    // Main function
    Graph aGraph(n, e);
    omp_set_num_threads(t);
    auto start = chrono::high_resolution_clock::now();
    aGraph.color();
    auto end = chrono::high_resolution_clock::now();
    cout << "--------------------------" << endl;
    chrono::duration<double> dur = end - start;
    cout << "Run time: "  << dur.count() << endl;
    cout << "Minimum number of colors required: " << aGraph.getMinColoring() << endl;
    cout << "--------------------------" << endl;
    aGraph.inspectGraph();
    aGraph.checkGraph();
    cout << "Done the main" << endl;
}