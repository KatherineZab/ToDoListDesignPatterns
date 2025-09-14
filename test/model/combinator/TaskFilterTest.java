package model.combinator;

import model.Filters;
import model.TaskFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskFilter} and {@link Filters}.
 * Verifies:
 * - any(): always true, including for null inputs.
 * - textContains(q): case-insensitive match on title/description; trims q; blank/null q behaves like any().
 * - stateIs(st): exact match; "ALL" disables state filtering.
 * - Logical combinators: AND / OR / NOT and complex chaining.
 */

@DisplayName("TaskFilter & Filters (Combinator) – Unit Tests")
class TaskFilterTest {

    private String testTitle;
    private String testDescription;
    private String testState;

    /**
     * Initializes common sample values used by multiple tests.
     * Title contains the word "bug"; description mentions "memory"; state is IN_PROGRESS.
     */
    @BeforeEach
    void setUp() {
        testTitle = "Fix critical bug";
        testDescription = "Resolve memory leak in user management";
        testState = "IN_PROGRESS";
    }

    /**
     * Ensures any() always returns {@code true}, regardless of inputs,
     * including empty strings and {@code null} values.
     */
    @Test
    @DisplayName("any() should always return true (including nulls)")
    void testAnyFilterAlwaysReturnsTrue() {
        TaskFilter any = Filters.any();
        assertTrue(any.test("", "", ""));
        assertTrue(any.test("title", "desc", "TO_DO"));
        assertTrue(any.test(null, null, null));
    }

    @Test
    @DisplayName("textContains is case-insensitive and trims input")
    void testTextContainsFilter() {
        TaskFilter filter = Filters.textContains("BUG");
        assertTrue(filter.test(testTitle, testDescription, testState)); // matches title

        TaskFilter memoryFilter = Filters.textContains("  MEMORY  ");
        assertTrue(memoryFilter.test(testTitle, testDescription, testState)); // matches description

        TaskFilter noMatch = Filters.textContains("database");
        assertFalse(noMatch.test(testTitle, testDescription, testState));
    }

    @Test
    @DisplayName("textContains with null/blank behaves like any()")
    void testTextContainsWithEmptyQuery() {
        TaskFilter nullFilter = Filters.textContains(null);
        TaskFilter emptyFilter = Filters.textContains("");
        TaskFilter spaceFilter = Filters.textContains("   ");
        assertTrue(nullFilter.test("title", "desc", "state"));
        assertTrue(emptyFilter.test("title", "desc", "state"));
        assertTrue(spaceFilter.test("title", "desc", "state"));
    }


    
    @Test
    @DisplayName("stateIs matches exactly; 'ALL' behaves like any()")
    void testStateIsFilter() {
        TaskFilter inProgress = Filters.stateIs("IN_PROGRESS");
        TaskFilter todo = Filters.stateIs("TO_DO");
        TaskFilter all = Filters.stateIs("ALL");

        assertTrue(inProgress.test(testTitle, testDescription, "IN_PROGRESS"));
        assertFalse(inProgress.test(testTitle, testDescription, "TO_DO"));

        assertFalse(todo.test(testTitle, testDescription, "IN_PROGRESS"));
        assertTrue(todo.test(testTitle, testDescription, "TO_DO"));

        assertTrue(all.test(testTitle, testDescription, "IN_PROGRESS"));
        assertTrue(all.test(testTitle, testDescription, "TO_DO"));
    }

    @Test
    @DisplayName("AND requires both conditions to be true")
    void testAndCombinator() {
        TaskFilter text = Filters.textContains("bug");
        TaskFilter state = Filters.stateIs("IN_PROGRESS");
        TaskFilter combined = text.and(state);

        assertTrue(combined.test(testTitle, testDescription, "IN_PROGRESS"));      // both
        assertFalse(combined.test(testTitle, testDescription, "TO_DO"));           // only text
        assertFalse(combined.test("other task", "other desc", "IN_PROGRESS"));     // only state
        assertFalse(combined.test("other task", "other desc", "TO_DO"));           // neither
    }

    @Test
    @DisplayName("OR requires at least one condition to be true")
    void testOrCombinator() {
        TaskFilter text = Filters.textContains("bug");
        TaskFilter state = Filters.stateIs("COMPLETED");
        TaskFilter combined = text.or(state);

        assertTrue(combined.test(testTitle, testDescription, "IN_PROGRESS"));      // text only
        assertTrue(combined.test("other", "desc", "COMPLETED"));                   // state only
        assertTrue(combined.test("debug bug", "fixing", "COMPLETED"));             // both
        assertFalse(combined.test("other task", "other desc", "TO_DO"));           // none
    }

    @Test
    @DisplayName("NOT inverts the original filter result")
    void testNotCombinator() {
        TaskFilter text = Filters.textContains("bug");
        TaskFilter notText = text.not();

        assertTrue(text.test(testTitle, testDescription, testState));
        assertFalse(notText.test(testTitle, testDescription, testState));

        assertFalse(text.test("clean code", "refactor", testState));
        assertTrue(notText.test("clean code", "refactor", testState));
    }

    @Test
    @DisplayName("Complex: (text AND state) OR (NOT text)")
    void testComplexCombination() {
        TaskFilter text = Filters.textContains("bug");
        TaskFilter state = Filters.stateIs("IN_PROGRESS");
        TaskFilter complex = text.and(state).or(text.not());

        assertTrue(complex.test("fix bug", "desc", "IN_PROGRESS"));   // first part true
        assertFalse(complex.test("fix bug", "desc", "TO_DO"));        // both parts false
        assertTrue(complex.test("clean code", "desc", "TO_DO"));      // NOT text == true
    }

    @Test
    @DisplayName("Chaining multiple combinators")
    void testMultipleCombinatorChaining() {
        TaskFilter f1 = Filters.textContains("urgent");
        TaskFilter f2 = Filters.stateIs("TO_DO");
        TaskFilter f3 = Filters.textContains("bug");
        TaskFilter chained = f1.and(f2).and(f3);

        assertTrue(chained.test("urgent bug fix", "desc", "TO_DO"));
        assertFalse(chained.test("urgent feature", "desc", "TO_DO"));
        assertFalse(chained.test("urgent bug fix", "desc", "IN_PROGRESS"));
    }
}
