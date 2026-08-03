package simpaths.data.filters;

import java.util.function.Predicate;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Indicator;

/// Collection of [Person] filters.
public class Filters {
    private Filters() {
    }

    /// Filter the requested gender.
    public static Predicate<Person> gender(Gender gender) {
        return p -> p.getDemMaleFlag() == gender;
    }

    /// Filter male persons.
    public static Predicate<Person> male() {
        return gender(Gender.Male);
    }

    /// Filter female persons.
    public static Predicate<Person> female() {
        return gender(Gender.Female);
    }

    /// Filter persons strictly younger than given age.
    public static Predicate<Person> youngerStrict(int age) {
        return p -> p.getDemAge() < age;
    }

    /// Filter given age range (both ends are included).
    public static Predicate<Person> ageRange(int from, int to) {
        return p -> p.getDemAge() >= from && p.getDemAge() <= to;
    }

    /// Filter persons with at least one child in the given age range (both
    /// ends included).
    public static Predicate<Person> hasChildInAgeRange(int from, int to) {
        return p -> p.getBenefitUnit().getIndicatorChildren(from, to) == Indicator.True;
    }
}
