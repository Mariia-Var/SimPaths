package simpaths.data.filters;

import simpaths.model.Person;
import microsim.statistics.ICollectionFilter;

import java.util.function.Predicate;

/// Filter women aged 20 to 65, with at least one child in the given age range.
public class FemalesWithChildrenByChildAgeCSfilter implements ICollectionFilter {

    private Predicate<Person> checks;

    public FemalesWithChildrenByChildAgeCSfilter(int ageFrom, int ageTo) {
        this.checks = Filters.female()
                .and(Filters.ageRange(20, 65))
                .and(Filters.hasChildInAgeRange(ageFrom, ageTo));
    }

    public boolean isFiltered(Object object) {
        return this.checks.test((Person) object);
    }
}
