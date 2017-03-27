package seedu.taskmanager.model;

import java.util.Set;
import java.util.logging.Logger;

import javafx.collections.transformation.FilteredList;
import seedu.taskmanager.commons.core.ComponentManager;
import seedu.taskmanager.commons.core.LogsCenter;
import seedu.taskmanager.commons.core.UnmodifiableObservableList;
import seedu.taskmanager.commons.events.model.FilePathChangedEvent;
import seedu.taskmanager.commons.events.model.TaskManagerChangedEvent;
import seedu.taskmanager.commons.exceptions.IllegalValueException;
import seedu.taskmanager.commons.util.CollectionUtil;
import seedu.taskmanager.commons.util.StringUtil;
import seedu.taskmanager.logic.parser.DateTimeUtil;
import seedu.taskmanager.model.task.ReadOnlyTask;
import seedu.taskmanager.model.task.Task;
import seedu.taskmanager.model.task.TaskDate;
import seedu.taskmanager.model.task.TaskUtil;
import seedu.taskmanager.model.task.UniqueTaskList;
import seedu.taskmanager.model.task.UniqueTaskList.TaskNotFoundException;

/**
 * Represents the in-memory model of the task manager data. All changes to any
 * model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final TaskManager taskManager;
    private final FilteredList<ReadOnlyTask> filteredTasks;

    /**
     * Initializes a ModelManager with the given taskManager and userPrefs.
     */
    public ModelManager(ReadOnlyTaskManager taskManager, UserPrefs userPrefs) {
        super();
        assert !CollectionUtil.isAnyNull(taskManager, userPrefs);

        logger.fine("Initializing with task manager: " + taskManager + " and user prefs " + userPrefs);

        this.taskManager = new TaskManager(taskManager);
        filteredTasks = new FilteredList<>(this.taskManager.getTaskList());
    }

    public ModelManager() {
        this(new TaskManager(), new UserPrefs());
    }

    @Override
    public void resetData(ReadOnlyTaskManager newData) {
        taskManager.resetData(newData);
        indicateTaskManagerChanged();
    }

    @Override
    public ReadOnlyTaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public void changeFilePath(String newPath) {
        raise(new FilePathChangedEvent(newPath));
    }

    /** Raises an event to indicate the model has changed */
    private void indicateTaskManagerChanged() {
        raise(new TaskManagerChangedEvent(taskManager));
    }

    @Override
    public synchronized void deleteTask(ReadOnlyTask target) throws TaskNotFoundException {
        taskManager.removeTask(target);
        indicateTaskManagerChanged();
    }

    @Override
    public synchronized void addTask(Task task) throws UniqueTaskList.DuplicateTaskException {
        taskManager.addTask(task);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    @Override
    public void updateTask(int filteredTaskListIndex, ReadOnlyTask editedTask)
            throws UniqueTaskList.DuplicateTaskException {
        assert editedTask != null;

        int taskManagerIndex = filteredTasks.getSourceIndex(filteredTaskListIndex);
        taskManager.updateTask(taskManagerIndex, editedTask);
        indicateTaskManagerChanged();
    }

    // =========== Filtered Task List Accessors
    // =============================================================

    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredTaskList() {
        return new UnmodifiableObservableList<>(filteredTasks);
    }

    @Override
    public void updateFilteredListToShowAll() {
        filteredTasks.setPredicate(null);
    }

    @Override
    public void updateFilteredTaskListByTaskName(Set<String> keywords) {
        updateFilteredTaskList(new PredicateExpression(new NameQualifier(keywords)));
    }

    @Override
    public void updateFilteredTaskListByTaskTypeOrDate(String taskType) {
        updateFilteredTaskList(new PredicateExpression(new TypeQualifier(taskType)));
    }

    @Override
    public void updateFilteredTaskListByTaskTypeAndDate(String[] taskTypeAndDate) {
        updateFilteredTaskList(new PredicateExpression(new TypeAndDateQualifier(taskTypeAndDate)));
    }

    private void updateFilteredTaskList(Expression expression) {
        filteredTasks.setPredicate(expression::satisfies);
    }

    // ========== Inner classes/interfaces used for filtering
    // =================================================

    interface Expression {
        boolean satisfies(ReadOnlyTask task);

        @Override
        String toString();
    }

    private class PredicateExpression implements Expression {

        private final Qualifier qualifier;

        PredicateExpression(Qualifier qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public boolean satisfies(ReadOnlyTask task) {
            return qualifier.run(task);
        }

        @Override
        public String toString() {
            return qualifier.toString();
        }
    }

    interface Qualifier {
        boolean run(ReadOnlyTask task);

        @Override
        String toString();
    }

    private class NameQualifier implements Qualifier {
        private Set<String> nameKeyWords;

        NameQualifier(Set<String> nameKeyWords) {
            this.nameKeyWords = nameKeyWords;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            return nameKeyWords.stream()
                    .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getName().fullName, keyword)).findAny()
                    .isPresent();
        }

        @Override
        public String toString() {
            return "name=" + String.join(", ", nameKeyWords);
        }
    }

    private class TypeQualifier implements Qualifier {
        private String taskType;

        TypeQualifier(String taskType) {
            this.taskType = taskType;
        }

        @Override
        public boolean run(ReadOnlyTask task) {
            switch (taskType) {
            case "floating":
                return TaskUtil.isFloating(task);
            case "deadline":
                return TaskUtil.isDeadline(task);
            case "event":
                return TaskUtil.isEvent(task);
            // for parsing date
            default:
                try {
                    TaskDate date = new TaskDate(DateTimeUtil.parseDateTime(taskType));
                    return task.getStartDate().get().getOnlyDate().equals(date.getOnlyDate())
                            || task.getEndDate().get().getOnlyDate().equals(date.getOnlyDate());

                } catch (IllegalValueException ive) {
                    // Deliberately empty as taskType will not throw exception
                    return false;
                }
            }
        }
    }

    private class TypeAndDateQualifier implements Qualifier {
        private String taskType;
        private TaskDate date;

        TypeAndDateQualifier(String[] taskTypeAndDate) {
            taskType = taskTypeAndDate[0];

            try {
            	date = new TaskDate(DateTimeUtil.parseDateTime(taskTypeAndDate[1]));
            } catch (IllegalValueException ive) {
            	// Deliberately empty as this date will not throw exception
            }
        }

        @Override
        public boolean run(ReadOnlyTask task) {

        	boolean dateFilter = task.getStartDate().get().getOnlyDate().equals(date.getOnlyDate())
                    || task.getEndDate().get().getOnlyDate().equals(date.getOnlyDate());

            switch (taskType) {
            case "floating":
                return TaskUtil.isFloating(task) && dateFilter;
            case "deadline":
                return TaskUtil.isDeadline(task) && dateFilter;
            case "event":
                return TaskUtil.isEvent(task) && dateFilter;
            default:
            	// will never reach this step
                return false;
            }
        }
    }
}
