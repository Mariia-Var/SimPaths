package simpaths.data.filters;

import java.util.function.Predicate;

import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.Region;

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

    /// Filter children (i.e. strictly younger than
    /// [Parameters#AGE_TO_BECOME_RESPONSIBLE]).
    public static Predicate<Person> child() {
        return youngerStrict(Parameters.AGE_TO_BECOME_RESPONSIBLE);
    }

    /// Filter by region.
    public static Predicate<Person> region(Region region) {
        return p -> p.getRegion() == region;
    }

    /// Filter by education.
    public static Predicate<Person> education(Education education) {
        return p -> p.getEduHighestC4() == education;
    }

    /// Filter employed persons.
    public static Predicate<Person> employed() {
        return p -> p.getLabC4() == Les_c4.EmployedOrSelfEmployed;
    }

    /// Filter by employment history.
    public static Predicate<Person> employmentHistory(Les_c4 employmentLag1) {
        return p -> p.getLabC4L1() == employmentLag1;
    }
}
