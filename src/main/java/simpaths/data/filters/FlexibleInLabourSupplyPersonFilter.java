package simpaths.data.filters;

import java.util.function.Predicate;

import simpaths.model.Person;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.Les_c4;

@Deprecated(forRemoval = true)
public class FlexibleInLabourSupplyPersonFilter<T extends Person> implements Predicate<T> {


	public FlexibleInLabourSupplyPersonFilter() {
		super();
	}

	@Override
	public boolean test(T person) {

		return (person.getDemAge() >= 18 && person.getDemAge() <= 64 &&
				person.getLabC4() != Les_c4.Student && person.getLabC4() != Les_c4.Retired &&
				person.getHealthDsblLongtermFlag() != Indicator.True);
	}


}
