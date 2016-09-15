var allFilter = angular.module('allFilter',[]);

allFilter.filter('hibdate',function() {
	return function(input) {
		if(input) {
			return input/60000+'m';
		}
	}
});