module
		.factory(
				'Paginator',
				function() {
					return function(fetchFunction, pageSize) {
						var paginator = {
							hasNextVar : false,
							fetch : function(page) {
								this.currentOffset = (page - 1) * pageSize;
								this._load();
							},
							next : function() {
								if (this.hasNextVar) {
									this.currentOffset += pageSize;
									this._load();
								}
							},
							_load : function() {
								var self = this; // must use self
								self.currentPage = Math
										.floor(self.currentOffset / pageSize) + 1;
								fetchFunction(
										this.currentOffset,
										pageSize + 1,
										function(data) {
											items = data.second;
											length = data.first;
											if (length == 0) {
												return;
											}
											self.totalPage = Math.ceil(length
													/ pageSize);
											self.endPage = self.totalPage;
											// 生成链接
											if (self.currentPage > 1
													&& self.currentPage < self.totalPage) {
												self.pages = [
														self.currentPage - 1,
														self.currentPage,
														self.currentPage + 1 ];
											} else if (self.currentPage == 1
													&& self.totalPage > 1) {
												self.pages = [
														self.currentPage,
														self.currentPage + 1 ];
											} else if (self.currentPage == self.totalPage
													&& self.totalPage > 1) {
												self.pages = [
														self.currentPage - 1,
														self.currentPage ];
											}
											self.currentPageItems = items
													.slice(0, pageSize);
											self.hasNextVar = items.length === pageSize + 1;
										});
							},
							hasNext : function() {
								return this.hasNextVar;
							},
							previous : function() {
								if (this.hasPrevious()) {
									this.currentOffset -= pageSize;
									this._load();
								}
							},
							hasPrevious : function() {
								return this.currentOffset !== 0;
							},
							totalPage : 1,
							pages : [],
							lastpage : 0,
							currentPage : 1,
							endPage : 1,

							currentPageItems : [],
							currentOffset : 0
						};

						// 加载第一页
						paginator._load();
						return paginator;
					};
				});

module.controller(
				'TopicSettingController',
				[
						'$rootScope',
						'$scope',
						'$http',
						'Paginator',
						'ngDialog',
						'$interval',
						function($rootScope, $scope, $http, Paginator,
								ngDialog, $interval) {
							var fetchFunction = function(offset, limit,
									callback) {
								var transFn = function(data) {
									return $.param(data);
								}
								var postConfig = {
									transformRequest : transFn
								};
								var data = {
									'offset' : offset,
									'limit' : limit
								};
								$http.get(window.contextPath + $scope.suburl, {
									params : {
										offset : offset,
										limit : limit
									}
								}).success(callback);
							};

							$scope.suburl = "/console/setting/topic/list";
							$scope.numrecord = 30;

							$scope.searchPaginator = Paginator(fetchFunction,
									$scope.numrecord);

							// for whitelist
							$scope.loadconsumerids = function(topic){
								
								$http(
										{
											method : 'GET',
											params : { topic: topic},
											url : window.contextPath
											+ '/console/setting/consumerids'
										}).success(
												function(data, status, headers, config) {
													$('#whitelist').tagsinput({
														typeahead : {
															items : 16,
															source : data,
															displayText : function(item) {
																return item;
															} // necessary
														}
													});
												}).error(
														function(data, status, headers, config) {
														});
							}
							$http({
								method : 'GET',
								url : window.contextPath + '/console/topic/namelist'
							}).success(function(data, status, headers, config) {
								var topicNameList = data;
								$("#topic").typeahead({
									items: 16, 
									source : topicNameList,
									updater : function(c) {
										$scope.topicEntry.topic = c;
										$scope.loadconsumerids($scope.topicEntry.topic);
										return c;
									}
								})
							}).error(function(data, status, headers, config) {
							});

							$scope.topicEntry = {};
							$scope.topicEntry.topic = "";
							$scope.topicEntry.producerpeak = "";
							$scope.topicEntry.producervalley = "";
							$scope.topicEntry.producerfluctuation = "";
							$scope.topicEntry.producerdelay = "";

							$scope.refreshpage = function(myForm) {
								if ($scope.topicEntry.producerpeak < $scope.topicEntry.producervalley) {
									alert("谷值不能小于峰值");
									return;
								}
								$('#myModal').modal('hide');
								var param = JSON.stringify($scope.topicEntry);

								$
										.ajax({
											type : "POST",
											url : window.contextPath
													+ '/console/setting/topic/create',
											contentType : "application/json; charset=utf-8",
											dataType : "json",
											data : param,
											success : function(data) {
												$scope.searchPaginator = Paginator(
														fetchFunction,
														$scope.numrecord);
											}

										});
							}

							$scope.clearModal = function() {
								$scope.topicEntry.topic = "";
								$scope.topicEntry.producerpeak = "";
								$scope.topicEntry.producervalley = "";
								$scope.topicEntry.producerfluctuation = "";
								$scope.topicEntry.producerFluctuationBase = "";
								$scope.topicEntry.producerdelay = "";
							}

							$scope.setModalInput = function(index) {
								$scope.topicEntry.topic = $scope.searchPaginator.currentPageItems[index].topic;
								$scope.topicEntry.producerpeak = $scope.searchPaginator.currentPageItems[index].producerpeak;
								$scope.topicEntry.producervalley = $scope.searchPaginator.currentPageItems[index].producervalley;
								$scope.topicEntry.producerfluctuation = $scope.searchPaginator.currentPageItems[index].producerfluctuation;
								$scope.topicEntry.producerdelay = $scope.searchPaginator.currentPageItems[index].producerdelay;
								$scope.topicEntry.producerFluctuationBase = $scope.searchPaginator.currentPageItems[index].producerFluctuationBase;
							}

							$rootScope.removerecord = function(cid) {
								$http.get(window.contextPath
														+ "/console/setting/topic/remove",
												{
													params : {
														topic : cid
													}
												})
										.success(
												function(data) {
													$scope.searchPaginator = Paginator(
															fetchFunction,
															$scope.numrecord);
												});
								return true;
							}

							$scope.dialog = function(cid) {
								$rootScope.cid = cid;
								ngDialog
										.open({
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

						} ]);
