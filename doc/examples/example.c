struct node {
  int data;
  struct node* next;
};

// parser does not handle NULL right now
struct node* NULL = 0;

int main() {
  int i = 0;
  int a = 0;
  struct node* ptr = malloc(sizeof(struct node));
  struct node* head = malloc(sizeof(struct node));
  ptr = head;

  while (1) {
    if (i == 20) {
       goto LOOPEND;
    } else {
       i++;
       a++;
    }

    if (i != a) {
      goto ERROR;
    }
  }

  LOOPEND:

  if (a != 20) {
     goto ERROR;
  }

  return (0);
  ERROR:
  return (-1);
}

