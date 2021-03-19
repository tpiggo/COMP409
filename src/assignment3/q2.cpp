#include <omp.h>
#include <iostream>
#include <vector>
using namespace std;

class Node
{
    public:
        int color;
        Node();
        ~Node();

    private:
        vector<Node> adj;
};


// My Main function
int main()
{

}