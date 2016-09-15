var myapp = angular.module('myApp',['ui.router','allCtrl','allFilter']);

myapp.config(function($stateProvider,$urlRouterProvider) {
	$urlRouterProvider.otherwise('/index');
	$stateProvider
		.state('index',{
			url: '/index',
			views: {
				'': {
					templateUrl: 'tpls/main.html'
				},
				'table@index': {
					templateUrl: 'tpls/table.html',
					controller: 'tableCtrl'
				}
			}
		})
});
myapp.config(function($httpProvider) {
	$httpProvider.defaults.useXDomain=true;
	delete $httpProvider.defaults.headers
		.common['X-Requested-With'];
});