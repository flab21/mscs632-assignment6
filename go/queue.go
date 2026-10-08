package main

import (
	"errors"
	"fmt"
)

// Task is one unit of work in the shared queue.
type Task struct {
	ID   int
	Data int
}

func (t Task) String() string {
	return fmt.Sprintf("Task#%d(data=%d)", t.ID, t.Data)
}

// ErrQueueEmpty is returned by GetTask once the queue is closed and drained.
var ErrQueueEmpty = errors.New("queue is empty and closed")

// TaskQueue wraps a buffered channel. The channel already handles the
// locking for us, so no mutex is needed here.
type TaskQueue struct {
	tasks chan Task
}

func NewTaskQueue(capacity int) *TaskQueue {
	return &TaskQueue{tasks: make(chan Task, capacity)}
}

// AddTask puts a task on the queue. It returns an error instead of
// blocking forever if the queue is full.
func (q *TaskQueue) AddTask(t Task) error {
	select {
	case q.tasks <- t:
		return nil
	default:
		return fmt.Errorf("queue full, could not add %v", t)
	}
}

// GetTask blocks until a task is ready. When the channel is closed
// and empty it returns ErrQueueEmpty so the worker knows to stop.
func (q *TaskQueue) GetTask() (Task, error) {
	t, ok := <-q.tasks
	if !ok {
		return Task{}, ErrQueueEmpty
	}
	return t, nil
}

// Close tells workers no more tasks are coming.
func (q *TaskQueue) Close() {
	close(q.tasks)
}
