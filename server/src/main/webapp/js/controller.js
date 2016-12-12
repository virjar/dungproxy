var allCtrl = angular.module('allCtrl',[]);

allCtrl.controller('tableCtrl',function($http,$scope) {
	$http.get("system/static")
		.success(function(data) {
			$scope.tabledata = data;
		})
		.error(function() {
			console.log("error");
		});
	$scope.isshow=false;
	$scope.showErrorinfo = function(isshow) {
		if (isshow==false) {
			return true;
		} else {
			return false;
		}
	};
});