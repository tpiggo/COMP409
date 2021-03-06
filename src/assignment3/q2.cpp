#include <omp.h>
#include <iostream>
#include <vector>
#include <cstdlib>
#include <algorithm>
#include <time.h>
#include <chrono>
#include <fstream>
#include <unordered_set>
using namespace std;

/**
 * TODO: Comment code!
 * 
 */ 
class TicketLock
{
    public:
        TicketLock(int numThreads)
        {
            turn = new int[numThreads];
        }

        ~TicketLock()
        {
            delete []turn;
        }

        bool enter() 
        {
            int i = 0;
            #pragma omp atomic capture
                turn[omp_get_thread_num()] = number++;
            while (turn[omp_get_thread_num()] != next);
        }

        void exit() 
        {
            next++;
        }

    private:
        int next = 0;
        int number = 0;
        int *turn;
};

class Node
{
    public:
        Node(int id)
        {
            this->id = id;
            this->color = 0;
        }

        unordered_set<Node *> getAdj() const
        {
            return adj;
        }

        void addNode(Node *pNode)
        {
            adj.insert(pNode);
        }

        int getId() const
        {
            return id;
        }

        int getColor()
        {
            int col = 0;
            #pragma omp atomic read
                col = this->color;
            return col;
        }

        void setColor(int color)
        {
            // may not need the lock
            #pragma omp atomic write
                this->color = color;
        }

        bool adjContains(Node *pNode)
        {
            return adj.find(pNode) != adj.end();
        }

    private:
        unordered_set<Node*> adj = {};
        int id;
        int color;
};

class Graph
{
    public:
        Graph(int numNodes, int numEdges, int numThreads);
        ~Graph();
        // Calling the parallelizable functions here.
        // Wrapping them up in this easy to use function for data hiding.
        void color();
        int getMinColoring();
        int getMinDegree();
        // inspector function
        void inspectGraph();
    private:
        vector<Node*> nodes = {};
        vector<Node*> conflicts = {};
        int numNodes;        
        // Here we make the parallelizable functions
        void assign();
        vector<Node*> detectConflict();
        vector<Node*> detectConflict2();
        // Ticket algorithm setup.
        TicketLock *lock;
};
// Not the most efficient way of building a graph :(
int a = 1;
Graph::Graph(int numNodes, int numEdges, int numThreads)
{
    for (int i = 1; i <= numNodes; i++)
    {
        nodes.push_back(new Node(i));
    }

    this->numNodes = numNodes;
    srand(time(NULL)+a);
    rand();
    cout << "number of edges: " << numEdges << endl;
    
    auto start = chrono::high_resolution_clock::now();
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
    }

    chrono::duration<double> dur = chrono::high_resolution_clock::now() - start;
    cout << "Time taken to create graph of " << numEdges << " was " << dur.count() << endl;
    // initializing the lock here!
    lock = new TicketLock(numThreads);
}

Graph::~Graph()
{
    while (this->nodes.size() !=0)
    {
        Node *back = nodes.at(nodes.size()-1);
        nodes.pop_back();
        delete back;
    }
    delete lock;
}

void Graph::inspectGraph()
{
    this->conflicts = nodes;
    this->conflicts = this->detectConflict();
    if (this->conflicts.size() != 0)
    {
        cout << "ERROR: You have conflicts!" << endl;
    }
    else 
    {
        cout << "Passed: No conflicts!" << endl;
    }

}

void Graph::color()
{
    // All nodes are conflicting right now.
    this->conflicts = nodes;
    int i = 0;
    while (this->conflicts.size() != 0 )
    {
        this->assign();
        this->conflicts = this->detectConflict();
        i++;
        if (i > this->numNodes)
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

int Graph::getMinDegree()
{
    int minD = this->nodes.size();
    for (Node *n: this->nodes)
    {
        if (minD > n->getAdj().size())
            minD = n->getAdj().size();
    }
    return minD;
}

void Graph::assign()
{
    #pragma omp parallel for
    for (int i=0; i < this->conflicts.size(); i++) {
        // Each thread should check its neighbours and make a decision
        Node *node = this->conflicts.at(i);
        int minC = 1; // Lowest possible color;
        unordered_set<int> colorSet;
        // Create a hash set of the colors adjacent to your node.
        for (Node *pNode : node->getAdj())
        {
            colorSet.insert(pNode->getColor());
        }

        for (int i = 1; i <= this->numNodes; i++)
        {
            if (colorSet.find(i) == colorSet.end()) 
            {
                minC = i;
                break;
            }
        }
        node->setColor(minC);
    }
}


// implement the ticket algorithm
vector<Node*> Graph::detectConflict()
{
    vector<Node*> newConflicts;
    #pragma omp parallel for
    for (int i = 0;i < this->conflicts.size(); i++) {
        // Each thread should check its neighbours and make a decision
        Node *node = this->conflicts.at(i);
        for (Node *pNode : node->getAdj())
        {

            if (pNode->getColor() == node->getColor())
            {
                // choose the max!
                if (pNode->getId() < node->getId())
                {
                    lock->enter();
                    newConflicts.push_back(node);
                    lock->exit();
                    break;
                }
            }
        }
    }

    return newConflicts;
}

// My Main function
int main(int argc, char *argv[])
{
    int t=1, n = 3, e = 1;
    if (argc>1) {
        n = atoi(argv[1]);
        printf("Using %d nodes. ",n);
        if (argc>2) {
            e = atoi(argv[2]);
            printf("Using %d edges. ", e);
            if (argc > 3)
            {
                t = atoi(argv[3]);
                printf("Using %d threads", t);
            }
        }
    }
    cout << endl;
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
    // Create the graph and set the threads
    Graph aGraph(n, e, t);
    omp_set_num_threads(t);

    cout << "=== start coloring ===" << endl;
    auto start = chrono::high_resolution_clock::now();
    aGraph.color();
    auto end = chrono::high_resolution_clock::now();
    cout << "=== end coloring ===" << endl;
    cout << "--------------------------" << endl;
    chrono::duration<double> dur = end - start;
    auto dur2 = end - start;
    cout << "Run time: "  << dur.count() << endl;
    cout << "Run time: "  << dur2/chrono::milliseconds(1) << "in ms." << endl;
    cout << "Minimum number of colors required: " << aGraph.getMinColoring() << endl;
    cout << "Minimum node degree: " << aGraph.getMinDegree() << endl;
    cout << "--------------------------" << endl;
    aGraph.inspectGraph();
    cout << "Done the main" << endl;
}