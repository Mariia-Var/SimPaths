package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

@Deprecated(forRemoval = true)
@SuppressWarnings("removal")
public class FemaleCSfilter implements ICollectionFilter{
	
	public boolean isFiltered(Object object) {
		return ( ((Person) object).getDemMaleFlag().equals(Gender.Female) );
	}
	
}
