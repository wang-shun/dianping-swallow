module.factory('Paginator', function(){
	return function(fetchFunction, pageSize, entity){
		var paginator = {
				hasNextVar: false,
				fetch: function(page){
					this.currentOffset = (page - 1) * pageSize;
					this._load();
				},
				next: function(){
					if(this.hasNextVar){
						this.currentOffset += pageSize;
						this._load();
					}
				},
				_load: function(){
					var self = this;  //must use  self
					self.currentPage = Math.floor(self.currentOffset/pageSize) + 1;
					entity.offset = this.currentOffset;
					entity.limit = pageSize + 1;
					fetchFunction(entity, function(data){
						items = data.second;
						length = data.first;
						if(length == 0){
							return;
						}
						self.totalPage = Math.ceil(length/pageSize);
						self.endPage = self.totalPage;
						//生成链接
						if (self.currentPage > 1 && self.currentPage < self.totalPage) {
							self.pages = [
			                    self.currentPage - 1,
			                    self.currentPage,
			                    self.currentPage + 1
			                ];
			            } else if (self.currentPage == 1 && self.totalPage > 1) {
			            	self.pages = [
			                    self.currentPage,
			                    self.currentPage + 1
			                ];
			            } else if (self.currentPage == self.totalPage && self.totalPage > 1) {
			            	self.pages = [
			                    self.currentPage - 1,
			                    self.currentPage
			                ];
			            }
						self.currentPageItems = items.slice(0, pageSize);
						self.hasNextVar = items.length === pageSize + 1;
					});
				},
				hasNext: function(){
					return this.hasNextVar;
				},
				previous: function(){
					if(this.hasPrevious()){
						this.currentOffset -= pageSize;
						this._load();
					}
				},
				hasPrevious: function(){
					return this.currentOffset !== 0;
				},
				totalPage: 1,
				pages : [],
				lastpage : 0,
				currentPage: 1,
				endPage: 1,
				
				currentPageItems: [],
				currentOffset: 0
		};
		
		//加载第一页
		paginator._load();
		return paginator;
	};
});

module.controller('MongoServerSettingController', ['$rootScope', '$scope', '$http', 'Paginator', 'ngDialog','$interval',
                function($rootScope, $scope, $http, Paginator, ngDialog,$interval){
	var fetchFunction = function(entity, callback){
		$http.post(window.contextPath + $scope.suburl, entity).success(callback);
	};
	
	$scope.suburl = "/console/server/mongo/list";
	$scope.numrecord = 30;
	
	$scope.entity = new Object();
	$scope.searchPaginator = Paginator(fetchFunction, $scope.numrecord, $scope.entity );
	
	//for whitelist
	
	$scope.mongoserverEntry = {};
	$scope.mongoserverEntry.ip;
	$scope.mongoserverEntry.catalog;
	$scope.mongoserverEntry.disk;
	$scope.mongoserverEntry.load;
	$scope.mongoserverEntry.qps;
	$scope.mongoserverEntry.topics;
	$scope.mongoserverEntry.groupName;
	
	$scope.refreshpage = function(myForm){
		$scope.mongoserverEntry.groupName = $('#groupName').val();
		$('#myModal').modal('hide');
		var param = JSON.stringify($scope.mongoserverEntry);
		
		$http.post(window.contextPath + '/console/server/mongo/create', $scope.mongoserverEntry).success(function(response) {
			$scope.searchPaginator = Paginator(fetchFunction, $scope.numrecord, $scope.entity);
    	});
    	
    }
	
	$scope.isReadOnly = false;
	$scope.clearModal = function(){
		$scope.isReadOnly = false;
		$scope.mongoserverEntry.ip = "";
		$scope.mongoserverEntry.catalog = "";
		$scope.mongoserverEntry.disk = "";
		$scope.mongoserverEntry.load = "";
		$scope.mongoserverEntry.qps = "";
		$scope.mongoserverEntry.topics = "";
		$scope.mongoserverEntry.groupName = "";
	}
	
	$scope.setModalInput = function(index){
		$scope.isReadOnly = true;
		if(typeof($scope.searchPaginator.currentPageItems[index].topics) != "undefined"){
			var topics = $scope.searchPaginator.currentPageItems[index].topics;
			$('#topics').tagsinput('removeAll');
			if(topics != null && topics.length > 0){
				var list = topics.split(",");
				for(var i = 0; i < list.length; ++i)
					$('#topics').tagsinput('add', list[i]);
			}
		}else{
			$('#topics').tagsinput('removeAll');
		}
		
		$('#groupName').val($scope.searchPaginator.currentPageItems[index].groupName);
		$scope.mongoserverEntry.id = $scope.searchPaginator.currentPageItems[index].id;
		$scope.mongoserverEntry.ip = $scope.searchPaginator.currentPageItems[index].ip;
		$scope.mongoserverEntry.catalog = $scope.searchPaginator.currentPageItems[index].catalog;
		$scope.mongoserverEntry.disk = $scope.searchPaginator.currentPageItems[index].disk;
		$scope.mongoserverEntry.load = $scope.searchPaginator.currentPageItems[index].load;
		$scope.mongoserverEntry.qps = $scope.searchPaginator.currentPageItems[index].qps;
	}
	
	$http({
		method : 'GET',
		url : window.contextPath + '/console/server/mongoip'
	}).success(function(data, status, headers, config) {
		var ips = data;
		$("#ip").typeahead({
			items: 16, 
			source : ips,
			updater : function(c) {
				$scope.mongoserverEntry.ip = c;
				return c;
			}
		})
	}).error(function(data, status, headers, config) {
	});

	$http({
		method : 'GET',
		url : window.contextPath + '/console/server/grouptype'
	}).success(function(data) {
		$scope.types = data;
	}).error(function(data, status, headers, config) {
	});

	$rootScope.removerecord = function(catalog){
		$http.get(window.contextPath + "/console/server/mongo/remove", {
			params : {
				catalog : catalog
			}
		}).success(function(data){
			$scope.searchPaginator = Paginator(fetchFunction, $scope.numrecord, $scope.entity);
		});
		return true;
	}
	
	
	$scope.dialog = function(cid) {
		$rootScope.cid = cid;
		ngDialog.open({
						template : '\
						<div class="widget-box">\
						<div class="widget-header">\
							<h4 class="widget-title">警告</h4>\
						</div>\
						<div class="widget-body">\
							<div class="widget-main">\
								<p class="alert alert-info">\
									您确认要删除吗？\
								</p>\
							</div>\
							<div class="modal-footer">\
								<button type="button" class="btn btn-default" ng-click="closeThisDialog()">取消</button>\
								<button type="button" class="btn btn-primary" ng-click="removerecord(cid)&&closeThisDialog()">确定</button>\
							</div>\
						</div>\
					</div>',
					plain : true,
					className : 'ngdialog-theme-default'
			});
	};

	$scope.isdefault = function(compare){
		return compare != "Default"; 
	}
}]);

